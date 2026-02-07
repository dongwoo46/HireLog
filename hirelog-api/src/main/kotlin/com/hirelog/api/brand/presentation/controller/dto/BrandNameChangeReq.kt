package com.hirelog.api.brand.presentation.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Brand 이름 변경 요청
 */
data class BrandNameChangeReq(

    @field:NotBlank
    @field:Size(max = 100)
    val name: String
)
