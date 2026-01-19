package com.hirelog.api.relation.application.jobsummary.query

import com.hirelog.api.relation.domain.model.MemberJobSummary

/**
 * MemberJobSummary Query Port
 *
 * 책임:
 * - MemberJobSummary 조회 유스케이스 정의
 * - 조회 전용 (Side Effect 없음)
 */
interface MemberJobSummaryQuery {

    /**
     * 사용자가 저장한 JD 목록 조회
     */
    fun findAllByMemberId(memberId: Long): List<MemberJobSummary>

    /**
     * 특정 JD를 저장한 사용자 목록 조회
     */
    fun findAllByJobSummaryId(jobSummaryId: Long): List<MemberJobSummary>

    /**
     * 단건 관계 조회
     *
     * - Facade에서 상태 변경 전 검증용
     */
    fun findByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummary?
}
