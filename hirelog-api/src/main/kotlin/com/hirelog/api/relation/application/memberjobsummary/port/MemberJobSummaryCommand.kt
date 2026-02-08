package com.hirelog.api.relation.application.memberjobsummary

import com.hirelog.api.relation.domain.model.MemberJobSummary

/**
 * MemberJobSummary Command (Write Port)
 *
 * 책임:
 * - MemberJobSummary Aggregate 조회
 * - 변경된 Aggregate 영속화
 *
 * 규칙:
 * - Entity 반환 허용
 * - View / Projection 반환 금지
 */
interface MemberJobSummaryCommand {

    /**
     * member + jobSummary 기준 Aggregate 조회
     *
     * - Write 전용
     * - 상태 변경, stage 변경에 사용
     */
    fun findEntityByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummary?

    /**
     * Aggregate 저장
     */
    fun save(memberJobSummary: MemberJobSummary)
}
