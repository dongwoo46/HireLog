package com.hirelog.api.position.application.view

import com.hirelog.api.position.domain.PositionStatus
import java.time.LocalDateTime

/**
 * PositionDetailView
 *
 * Position 상세 조회용 View
 */
data class PositionDetailView(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val status: PositionStatus,
    val description: String?,
    val createdAt: LocalDateTime,
    val category: PositionCategoryView?
)
