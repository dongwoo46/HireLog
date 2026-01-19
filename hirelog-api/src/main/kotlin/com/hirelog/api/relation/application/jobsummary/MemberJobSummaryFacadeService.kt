package com.hirelog.api.relation.application.jobsummary.facade

import com.hirelog.api.relation.application.jobsummary.command.MemberJobSummaryWriteService
import com.hirelog.api.relation.application.jobsummary.query.MemberJobSummaryQuery
import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import org.springframework.stereotype.Service

/**
 * MemberJobSummary Facade Service
 *
 * 책임:
 * - 회원-JD 요약 관계 쓰기 유스케이스 오케스트레이션
 * - 저장 정책 및 상태 변경 정책 결정
 *
 * 설계 원칙:
 * - 트랜잭션 ❌ (WriteService에서만 처리)
 * - 조회 API 제공 ❌
 */
@Service
class MemberJobSummaryFacadeService(
    private val memberJobSummaryQuery: MemberJobSummaryQuery,
    private val memberJobSummaryWriteService: MemberJobSummaryWriteService
) {

    /**
     * JD 저장
     *
     * 정책:
     * - 동일 회원-JD 요약 중복 저장 불가
     */
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

        return memberJobSummaryWriteService.create(relation)
    }

    /**
     * 저장 목적 변경
     *
     * 정책:
     * - 관계가 반드시 존재해야 함
     */
    fun changeSaveType(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType
    ) {
        val relation = memberJobSummaryQuery
            .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?: throw IllegalArgumentException(
                "MemberJobSummary not found. member=$memberId jobSummary=$jobSummaryId"
            )

        relation.changeSaveType(saveType)

        memberJobSummaryWriteService.update(relation)
    }

    /**
     * 메모 수정
     *
     * 정책:
     * - 관계가 반드시 존재해야 함
     */
    fun updateMemo(
        memberId: Long,
        jobSummaryId: Long,
        memo: String?
    ) {
        val relation = memberJobSummaryQuery
            .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?: throw IllegalArgumentException(
                "MemberJobSummary not found. member=$memberId jobSummary=$jobSummaryId"
            )

        relation.updateMemo(memo)

        memberJobSummaryWriteService.update(relation)
    }

    /**
     * 저장 취소
     *
     * 정책:
     * - 관계가 존재할 경우에만 삭제
     */
    fun delete(
        memberId: Long,
        jobSummaryId: Long
    ) {
        memberJobSummaryQuery
            .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?.let { memberJobSummaryWriteService.delete(it) }
    }
}
