package com.hirelog.api.job.presentation.controller.dto.request

import com.hirelog.api.job.application.summary.query.JobSummarySearchQuery
import com.hirelog.api.job.domain.type.CareerType

/**
 * JobSummary 검색 요청
 */
data class JobSummarySearchReq(
    val keyword: String? = null,
    val careerType: String? = null,

    // ID 필터
    val brandId: Long? = null,
    val companyId: Long? = null,
    val positionId: Long? = null,
    val brandPositionId: Long? = null,
    val positionCategoryId: Long? = null,

    // Name 필터 (match)
    val brandName: String? = null,
    val positionName: String? = null,
    val brandPositionName: String? = null,
    val positionCategoryName: String? = null,

    val techStacks: List<String>? = null,

    /**
     * Search After 커서
     * - 생략 또는 null: 첫 페이지
     * - 이전 응답의 nextCursor 값: 다음 페이지
     */
    val cursor: String? = null,

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
            brandName = brandName?.takeIf { it.isNotBlank() },
            positionName = positionName?.takeIf { it.isNotBlank() },
            brandPositionName = brandPositionName?.takeIf { it.isNotBlank() },
            positionCategoryName = positionCategoryName?.takeIf { it.isNotBlank() },
            techStacks = techStacks?.takeIf { it.isNotEmpty() },
            cursor = cursor,
            size = size.coerceIn(1, 100),
            sortBy = runCatching {
                JobSummarySearchQuery.SortBy.valueOf(sortBy)
            }.getOrDefault(JobSummarySearchQuery.SortBy.CREATED_AT_DESC)
        )
    }
}
