package com.hirelog.api.position.application.view


/**
 * Position 상세 조회용 View
 *
 * 목적:
 * - 관리/편집
 * - 검증
 *
 * 특징:
 * - 모든 도메인 정보 노출
 */
data class PositionDetailView(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val status: String,
    val description: String?,
    val category: PositionCategoryView
)


/**
 * PositionCategory 조회 전용 View
 *
 * - 목록/상세 공용
 */
data class PositionCategoryView(
    val id: Long,
    val name: String,
    val description: String?
)
