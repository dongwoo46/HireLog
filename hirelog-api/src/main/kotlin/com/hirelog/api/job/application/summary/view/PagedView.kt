package com.hirelog.api.job.application.review.port

/**
 * PagedResult
 *
 * 책임:
 * - 조회 결과 페이징 정보 캡슐화
 */
data class PagedView<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)
