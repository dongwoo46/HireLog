package com.hirelog.api.job.application.summary.query

import java.time.LocalDateTime

/**
 * JobSummary 검색 결과
 */
data class JobSummarySearchResult(
    val items: List<JobSummarySearchItem>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
) {
    companion object {
        fun of(
            items: List<JobSummarySearchItem>,
            totalCount: Long,
            page: Int,
            size: Int
        ): JobSummarySearchResult {
            val totalPages = if (totalCount == 0L) 0 else ((totalCount - 1) / size + 1).toInt()
            return JobSummarySearchResult(
                items = items,
                totalCount = totalCount,
                page = page,
                size = size,
                totalPages = totalPages
            )
        }
    }
}

/**
 * 검색 결과 항목
 *
 * 목록 조회에 필요한 최소 필드만 포함
 * 상세 데이터는 GET /api/job-summary/{id}로 조회
 */
data class JobSummarySearchItem(
    val id: Long,
    val brandName: String,
    val brandPositionName: String?,
    val positionCategoryName: String,
    val careerType: String,
    val summaryText: String,
    val techStackParsed: List<String>?,
    val createdAt: LocalDateTime
)
