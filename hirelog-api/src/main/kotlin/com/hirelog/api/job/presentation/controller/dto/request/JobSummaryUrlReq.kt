package com.hirelog.api.job.presentation.controller.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

data class JobSummaryUrlReq(

    @field:NotBlank(message = "브랜드명은 필수입니다")
    @field:Size(max = 200, message = "브랜드명은 200자를 초과할 수 없습니다")
    val brandName: String,

    @field:NotBlank(message = "포지션명은 필수입니다")
    @field:Size(max = 200, message = "포지션명은 200자를 초과할 수 없습니다")
    val brandPositionName: String,

    @field:NotBlank(message = "url은 필수입니다")
    @field:URL(message = "유효한 URL 형식이 아닙니다")
    val url: String
)
