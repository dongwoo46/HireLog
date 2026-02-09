package com.hirelog.api.job.application.summary.query

import com.hirelog.api.job.domain.type.CareerType

/**
 * JobSummary 검색 쿼리
 *
 * 검색 전략:
 * - keyword: best_fields + should + minimum_should_match
 * - ID 필터: term (filter context, AND)
 * - Name 필터: match (filter context, AND)
 */
data class JobSummarySearchQuery(
    /**
     * 키워드 검색어
     * - null/blank: 전체 조회
     * - 한글/영어 혼합 가능
     */
    val keyword: String? = null,

    /**
     * 경력 유형 필터
     */
    val careerType: CareerType? = null,

    // === ID 필터 (term) ===
    val brandId: Long? = null,
    val companyId: Long? = null,
    val positionId: Long? = null,
    val brandPositionId: Long? = null,
    val positionCategoryId: Long? = null,

    // === Name 필터 (match) ===
    val brandName: String? = null,
    val positionName: String? = null,
    val brandPositionName: String? = null,
    val positionCategoryName: String? = null,

    /**
     * 기술스택 필터 (정확한 매칭)
     * - techStackParsed 필드에서 검색
     */
    val techStacks: List<String>? = null,

    val page: Int = 0,
    val size: Int = 20,
    val sortBy: SortBy = SortBy.CREATED_AT_DESC
) {
    enum class SortBy {
        CREATED_AT_DESC,    // 최신순
        CREATED_AT_ASC,     // 오래된순
        RELEVANCE           // 검색 관련도순 (keyword 있을 때만 유효)
    }

    init {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..100) { "size must be between 1 and 100" }
    }
}
