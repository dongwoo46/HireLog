package com.hirelog.api.position.application.view

import com.hirelog.api.position.domain.PositionStatus

/**
 * PositionSummaryView
 *
 * Position 목록 조회용 최소 정보
 */
data class PositionSummaryView(
    val id: Long,
    val name: String,
    val status: PositionStatus,
    val categoryId: Long,
    val categoryName: String
)
