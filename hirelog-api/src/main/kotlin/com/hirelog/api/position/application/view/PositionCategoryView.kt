package com.hirelog.api.position.application.view

import com.hirelog.api.position.domain.PositionStatus

/**
 * PositionCategoryView
 *
 * PositionCategory 조회 전용 Read Model
 */
data class PositionCategoryView(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val status: PositionStatus,
    val description: String?
)
