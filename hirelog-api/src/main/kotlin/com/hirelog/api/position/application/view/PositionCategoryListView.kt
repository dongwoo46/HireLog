package com.hirelog.api.position.application.view

/**
 * PositionCategory 목록 조회용 View
 *
 * 사용처:
 * - 관리자 목록
 * - 검색 결과
 *
 * 특징:
 * - 조회 전용
 * - Entity 비의존
 * - QueryDSL constructor projection 전용
 */
data class PositionCategoryListView(
    val id: Long,
    val name: String,
    val status: String
)
