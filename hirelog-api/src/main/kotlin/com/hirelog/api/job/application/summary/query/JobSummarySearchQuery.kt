package com.hirelog.api.job.application.summary.query

import com.hirelog.api.job.domain.CareerType

/**
 * JobSummary 검색 쿼리
 *
 * 검색 대상 필드:
 * - positionName, brandName, companyName, brandPositionName
 * - summaryText, responsibilities, requiredQualifications
 * - preferredQualifications, techStack
 * - Insight 필드들
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

    /**
     * 브랜드 ID 필터
     */
    val brandId: Long? = null,

    /**
     * 회사 ID 필터
     */
    val companyId: Long? = null,

    /**
     * 포지션 ID 필터
     */
    val positionId: Long? = null,

    /**
     * 기술스택 필터 (정확한 매칭)
     * - techStackParsed 필드에서 검색
     */
    val techStacks: List<String>? = null,

    /**
     * 페이지 번호 (0부터 시작)
     */
    val page: Int = 0,

    /**
     * 페이지 크기
     */
    val size: Int = 20,

    /**
     * 정렬 기준
     */
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
