package com.hirelog.api.position.application.view


/**
 * Position 요약 View (Query 전용)
 *
 * 사용처:
 * - 목록 조회
 * - 검색 결과
 * - normalizedName 기반 조회
 *
 * 특징:
 * - 불변
 * - Entity 비의존
 * - QueryDSL constructor projection 전용
 */
class PositionView(
    val id: Long,
    val name: String,
    val status: String,
    val category: PositionCategoryView
)
