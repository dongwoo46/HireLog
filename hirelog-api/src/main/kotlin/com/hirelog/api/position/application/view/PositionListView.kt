package com.hirelog.api.position.application.view

/**
 * Position 목록 조회용 View
 *
 * 목적:
 * - 관리 화면 리스트
 * - 검색 결과
 *
 * 특징:
 * - 핵심 정보만 포함
 * - 가볍게 유지
 */
data class PositionListView(
    val id: Long,
    val name: String,
    val status: String,
)
