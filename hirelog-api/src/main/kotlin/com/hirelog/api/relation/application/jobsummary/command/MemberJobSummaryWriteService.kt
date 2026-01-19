package com.hirelog.api.relation.application.jobsummary.command

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.relation.application.jobsummary.query.MemberJobSummaryQuery
import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import jakarta.persistence.EntityExistsException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * MemberJobSummary Write Service
 *
 * 책임:
 * - MemberJobSummary 쓰기 트랜잭션 경계 정의
 * - Command Port 호출 위임
 *
 * 주의:
 * - 중복 정책 ❌
 * - 조회 판단 ❌
 */
@Service
class MemberJobSummaryWriteService(
    private val memberJobSummaryCommand: MemberJobSummaryCommand,
    private val memberJobSummaryQuery: MemberJobSummaryQuery
) {

    /**
     * JD 저장
     *
     * 정책:
     * - 동일 회원-JD 요약 중복 저장 불가
     */
    @Transactional
    fun save(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType,
        memo: String?
    ): MemberJobSummary {

        require(
            memberJobSummaryQuery
                .findByMemberIdAndJobSummaryId(memberId, jobSummaryId) == null
        ) {
            "MemberJobSummary already exists. member=$memberId jobSummary=$jobSummaryId"
        }

        val relation = MemberJobSummary.create(
            memberId = memberId,
            jobSummaryId = jobSummaryId,
            saveType = saveType,
            memo = memo
        )

        return try {
            memberJobSummaryCommand.save(relation)
        } catch (ex: DataIntegrityViolationException) {
            // 동시성 중복 → 도메인 예외로 번역
            throw EntityAlreadyExistsException(
                "MemberJobSummary already exists. member=$memberId jobSummary=$jobSummaryId",
                ex
            )
        }
    }

    /**
     * 저장 목적 변경
     *
     * 정책:
     * - 관계가 반드시 존재해야 함
     */
    @Transactional
    fun changeSaveType(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType
    ) {
        val relation = requireNotNull(
            memberJobSummaryQuery
                .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)
        ) {
            "MemberJobSummary not found. member=$memberId jobSummary=$jobSummaryId"
        }

        relation.changeSaveType(saveType)
    }

    /**
     * 메모 수정
     */
    @Transactional
    fun updateMemo(
        memberId: Long,
        jobSummaryId: Long,
        memo: String?
    ) {
        val relation = requireNotNull(
            memberJobSummaryQuery
                .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)
        ) {
            "MemberJobSummary not found. member=$memberId jobSummary=$jobSummaryId"
        }

        relation.updateMemo(memo)
    }

    /**
     * 저장 취소
     *
     * 정책:
     * - 존재할 경우에만 삭제
     */
    @Transactional
    fun delete(
        memberId: Long,
        jobSummaryId: Long
    ) {
        memberJobSummaryQuery
            .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?.let { memberJobSummaryCommand.delete(it) }
    }
}

