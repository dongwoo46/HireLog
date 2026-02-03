package com.hirelog.api.relation.application.jobsummary

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.relation.application.jobsummary.command.MemberJobSummaryCommand
import com.hirelog.api.relation.application.jobsummary.query.MemberJobSummaryQuery
import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
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
     * - 동일 member-jobSummary 조합은 단 하나만 존재
     * - 중복은 DB 제약으로 방지
     */
    @Transactional
    fun save(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType,
        memo: String?
    ): MemberJobSummary {

        val relation = MemberJobSummary.create(
            memberId = memberId,
            jobSummaryId = jobSummaryId,
            saveType = saveType,
            memo = memo
        )

        return try {
            memberJobSummaryCommand.save(relation)
        } catch (ex: DataIntegrityViolationException) {
            // UNIQUE(member_id, job_summary_id) 위반
            throw EntityAlreadyExistsException(
                entityName = "MemberJobSummary",
                identifier = "member=$memberId, jobSummary=$jobSummaryId",
                cause = ex
            )
        }
    }

    /**
     * 저장 목적 변경
     *
     * 정책:
     * - 관계는 반드시 존재해야 함
     */
    @Transactional
    fun changeSaveType(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType
    ) {
        val relation = memberJobSummaryCommand
            .findEntityByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?: throw EntityNotFoundException(
                entityName = "MemberJobSummary",
                identifier = "member=$memberId, jobSummary=$jobSummaryId"
            )

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
        val relation = memberJobSummaryCommand
            .findEntityByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?: throw EntityNotFoundException(
                entityName = "MemberJobSummary",
                identifier = "member=$memberId, jobSummary=$jobSummaryId"
            )

        relation.updateMemo(memo)
    }

    /**
     * 저장 취소
     *
     * 정책:
     * - idempotent delete
     */
    @Transactional
    fun delete(
        memberId: Long,
        jobSummaryId: Long
    ) {
        memberJobSummaryCommand
            .findEntityByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?.let { memberJobSummaryCommand.delete(it) }
    }
}


