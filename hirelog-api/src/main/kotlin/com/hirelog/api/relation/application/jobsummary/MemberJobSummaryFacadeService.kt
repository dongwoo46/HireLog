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
    private val memberJobSummaryWriteService: MemberJobSummaryWriteService
) {

    /**
     * JD 저장
     */
    fun save(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType,
        memo: String?
    ): MemberJobSummary =
        memberJobSummaryWriteService.save(
            memberId = memberId,
            jobSummaryId = jobSummaryId,
            saveType = saveType,
            memo = memo
        )

    /**
     * 저장 목적 변경
     */
    fun changeSaveType(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType
    ) {
        memberJobSummaryWriteService.changeSaveType(
            memberId = memberId,
            jobSummaryId = jobSummaryId,
            saveType = saveType
        )
    }

    /**
     * 메모 수정
     */
    fun updateMemo(
        memberId: Long,
        jobSummaryId: Long,
        memo: String?
    ) {
        memberJobSummaryWriteService.updateMemo(
            memberId = memberId,
            jobSummaryId = jobSummaryId,
            memo = memo
        )
    }

    /**
     * 저장 취소
     */
    fun delete(
        memberId: Long,
        jobSummaryId: Long
    ) {
        memberJobSummaryWriteService.delete(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        )
    }
}
