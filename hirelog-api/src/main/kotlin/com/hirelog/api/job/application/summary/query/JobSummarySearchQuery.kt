package com.hirelog.api.job.application.summary.query

import com.hirelog.api.job.domain.type.CareerType

data class JobSummarySearchQuery(
    val keyword: String? = null,
    val careerTypes: List<CareerType>? = null,

    val brandIds: List<Long>? = null,
    val companyIds: List<Long>? = null,
    val positionIds: List<Long>? = null,
    val brandPositionIds: List<Long>? = null,
    val positionCategoryIds: List<Long>? = null,

    val brandNames: List<String>? = null,
    val positionNames: List<String>? = null,
    val brandPositionNames: List<String>? = null,
    val positionCategoryNames: List<String>? = null,

    val techStacks: List<String>? = null,
    val companyDomains: List<String>? = null,
    val companySizes: List<String>? = null,
    val cursor: String? = null,
    val size: Int = 20,
    val sortBy: SortBy = SortBy.CREATED_AT_DESC
) {
    enum class SortBy {
        CREATED_AT_DESC,
        CREATED_AT_ASC,
        RELEVANCE,
        SAVE_COUNT_DESC
    }

    init {
        require(size in 1..100) { "size must be between 1 and 100" }
    }
}
