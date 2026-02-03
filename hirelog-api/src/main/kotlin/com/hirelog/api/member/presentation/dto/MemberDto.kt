package com.hirelog.api.member.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 프로필 수정 요청
 */
data class UpdateProfileReq(
    val currentPositionId: Long?,
    val careerYears: Int?,
    @field:Size(max = 1000)
    val summary: String?
)

/**
 * 표시 이름 변경 요청
 */
data class UpdateDisplayNameReq(
    @field:NotBlank
    val displayName: String
)
