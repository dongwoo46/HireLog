package com.hirelog.api.common.application.port

import kotlin.math.ceil

/**
 * Offset 기반 페이징 결과
 *
 * 규칙:
 * - page는 0-based
 * - totalPages = ceil(totalElements / size)
 * - hasNext = page + 1 < totalPages
 */
data class PagedResult<T> private constructor(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
) {

    companion object {

        /**
         * Offset 기반 PagedResult 생성 팩토리
         *
         * 생성 규칙을 단일 지점에서 강제한다.
         */
        fun <T> of(
            items: List<T>,
            page: Int,
            size: Int,
            totalElements: Long
        ): PagedResult<T> {

            require(page >= 0) { "page must be >= 0" }
            require(size > 0) { "size must be > 0" }
            require(totalElements >= 0) { "totalElements must be >= 0" }

            val totalPages = if (totalElements == 0L) {
                0
            } else {
                ceil(totalElements.toDouble() / size).toInt()
            }

            val hasNext = page + 1 < totalPages

            return PagedResult(
                items = items,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages,
                hasNext = hasNext
            )
        }

        /**
         * 비어있는 결과 생성 (optional helper)
         */
        fun <T> empty(page: Int, size: Int): PagedResult<T> =
            of(
                items = emptyList(),
                page = page,
                size = size,
                totalElements = 0
            )
    }

    /**
     * 결과 매핑 (페이지 메타데이터 유지)
     */
    fun <R> map(transform: (T) -> R): PagedResult<R> =
        PagedResult(
            items = items.map(transform),
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = hasNext
        )
}
