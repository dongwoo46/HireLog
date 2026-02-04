package com.hirelog.api.job.presentation.controller.dto

import com.hirelog.api.job.application.summary.query.JobSummarySearchQuery
import com.hirelog.api.job.domain.CareerType

/**
 * JobSummary 검색 요청
 */
data class JobSummarySearchReq(
    val keyword: String? = null,
    val careerType: String? = null,
    val brandId: Long? = null,
    val companyId: Long? = null,
    val positionId: Long? = null,
    val brandPositionId: Long? = null,
    val positionCategoryId: Long? = null,
    val techStacks: List<String>? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "CREATED_AT_DESC"
) {
    fun toQuery(): JobSummarySearchQuery {
        return JobSummarySearchQuery(
            keyword = keyword?.takeIf { it.isNotBlank() },
            careerType = careerType?.let { CareerType.valueOf(it) },
            brandId = brandId,
            companyId = companyId,
            positionId = positionId,
            brandPositionId = brandPositionId,
            positionCategoryId = positionCategoryId,
            techStacks = techStacks?.takeIf { it.isNotEmpty() },
            page = page,
            size = size.coerceIn(1, 100),
            sortBy = runCatching {
                JobSummarySearchQuery.SortBy.valueOf(sortBy)
            }.getOrDefault(JobSummarySearchQuery.SortBy.CREATED_AT_DESC)
        )
    }
}
