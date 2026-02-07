package com.hirelog.api.relation.application.memberjobsummary.port

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.relation.application.memberjobsummary.view.MemberJobSummaryDetailView
import com.hirelog.api.relation.application.memberjobsummary.view.MemberJobSummaryListView
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType

/**
 * MemberJobSummary Port (Read)
 *
 * 책임:
 * - MemberJobSummary 조회 전용
 * - View 기반 Projection 반환
 *
 * 규칙:
 * - Entity 반환 금지
 * - 필요한 컬럼만 조회
 */
interface MemberJobSummaryQuery {

    /**
     * 내가 저장한 JD 목록 조회
     */
    fun findMySummaries(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        page: Int,
        size: Int
    ): PagedResult<MemberJobSummaryListView>

    /**
     * 저장된 JD 상세 조회
     *
     * - 채용 단계 포함
     */
    fun findDetail(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummaryDetailView

    /**
     * 특정 JD 저장 여부 확인
     */
    fun exists(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean
}
