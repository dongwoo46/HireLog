package com.hirelog.api.job.presentation.controller.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 관리자 검증 요청 DTO
 *
 * 책임:
 * - 관리자 액션 수행 전 비밀번호 재확인
 */
data class VerifyAdminReq(

    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val password: String
)
