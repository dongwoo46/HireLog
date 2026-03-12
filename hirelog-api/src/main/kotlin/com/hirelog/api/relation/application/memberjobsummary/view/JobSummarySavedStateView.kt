package com.hirelog.api.relation.application.memberjobsummary.view

import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType

/**
 * JobSummary 목록 검색 시 현재 사용자의 저장 상태
 *
 * 용도:
 * - JobSummaryReadService의 two-query enrichment 전용
 * - jobSummaryId를 키로 Map에서 사용
 */
data class JobSummarySavedStateView(
    val jobSummaryId: Long,
    val memberJobSummaryId: Long,
    val saveType: MemberJobSummarySaveType
)
