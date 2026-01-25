package com.hirelog.api.job.application.summary.query

/**
 * JobSummary 검색 조건 (Query Model)
 *
 * 특징:
 * - HTTP와 무관
 * - 저장소 기술과 무관
 * - 조회 유스케이스를 표현
 */
data class JobSummarySearchCondition(
    val brandId: Long?,
    val positionId: Long?,
    val keyword: String?
)
