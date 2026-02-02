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
 * 리스트 조회에 필요한 필드만 포함
 */
data class JobSummarySearchItem(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val companyName: String?,
    val positionId: Long,
    val positionName: String,
    val brandPositionName: String?,
    val careerType: String,
    val careerYears: String?,
    val summaryText: String,
    val techStack: String?,
    val techStackParsed: List<String>?,
    val createdAt: LocalDateTime
)
