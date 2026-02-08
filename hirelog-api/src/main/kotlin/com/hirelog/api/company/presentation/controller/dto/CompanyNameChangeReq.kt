package com.hirelog.api.company.presentation.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Company 이름 변경 요청
 */
data class CompanyNameChangeReq(

    @field:NotBlank
    @field:Size(max = 200)
    val name: String
)