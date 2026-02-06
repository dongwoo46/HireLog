package com.hirelog.api.relation.application.jobsummary.query

import com.hirelog.api.relation.application.jobsummary.view.MemberJobSummaryView
import com.hirelog.api.relation.application.jobsummary.view.SavedJobSummaryView
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import com.hirelog.api.common.application.port.PagedResult

/**
 * MemberJobSummary Query Port
 *
 * 책임:
 * - MemberJobSummary 조회 유스케이스 정의
 * - 조회 전용 (Side Effect 없음)
 */
interface MemberJobSummaryQuery {

    fun findAllByMemberId(
        memberId: Long,
        page: Int,
        size: Int
    ): PagedResult<MemberJobSummaryView>

    fun existsByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean

    /**
     * 사용자가 저장한 JobSummary 목록 조회 (JobSummary Join)
     *
     * @param memberId 사용자 ID
     * @param saveType 저장 유형 필터 (null이면 전체)
     * @param page 페이지 번호 (0-based)
     * @param size 페이지 크기
     */
    fun findSavedJobSummaries(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        page: Int,
        size: Int
    ): PagedResult<SavedJobSummaryView>
}
