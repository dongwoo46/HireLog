package com.hirelog.api.job.application.summary.query

import java.time.LocalDateTime

/**
 * JobSummary 검색 결과 (무한스크롤 Search After 방식)
 *
 * - hasNext: size+1 fetch로 다음 페이지 존재 여부 판별
 * - nextCursor: 다음 페이지 요청 시 cursor 파라미터로 전달
 *   null이면 마지막 페이지
 */
data class JobSummarySearchResult(
    val items: List<JobSummarySearchItem>,
    val size: Int,
    val hasNext: Boolean,
    val nextCursor: String?
)

/**
 * 검색 결과 항목
 *
 * 목록 조회에 필요한 최소 필드만 포함
 * 상세 데이터는 GET /api/job-summary/{id}로 조회
 *
 * isSaved / memberJobSummaryId / memberSaveType:
 * - OpenSearch 검색 후 DB two-query enrichment로 채워짐
 * - 기본값은 미저장 상태
 */
data class JobSummarySearchItem(
    val id: Long,
    val brandName: String,
    val brandPositionName: String?,
    val positionCategoryName: String,
    val careerType: String,
    val summaryText: String,
    val techStackParsed: List<String>?,
    val createdAt: LocalDateTime,

    val isSaved: Boolean = false,
    val memberJobSummaryId: Long? = null,
    val memberSaveType: String? = null
)
