package com.hirelog.api.position.presentation.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * Position 생성 요청
 */
data class PositionCreateReq(
    @field:NotBlank
    val name: String,
    @field:NotNull
    val categoryId: Long,
    val description: String? = null
)

/**
 * PositionCategory 생성 요청
 */
data class PositionCategoryCreateReq(
    @field:NotBlank
    val name: String,
    val description: String? = null
)
