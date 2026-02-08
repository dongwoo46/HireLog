package com.hirelog.api.position.application.view

/**
 * PositionCategory 상세 조회용 View
 *
 * 사용처:
 * - 관리자 상세
 * - 수정 화면
 *
 * 특징:
 * - 조회 전용
 * - 모든 도메인 정보 포함
 * - Entity 비의존
 */
data class PositionCategoryDetailView(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val status: String,
    val description: String?
)
