package com.hirelog.api.brand.presentation.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Brand 생성 요청 DTO
 *
 * 용도:
 * - 관리자 수동 Brand 생성
 */
data class BrandCreateReq(

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,


    /**
     * 소속 회사 ID (선택)
     */
    val companyId: Long? = null,

)
