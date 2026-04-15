package com.hirelog.api.job.presentation.controller.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class JobSummaryAdminCreateFromImagesReq(

    @field:NotBlank(message = "브랜드명은 필수입니다")
    @field:Size(max = 200, message = "브랜드명은 200자 이내여야 합니다")
    val brandName: String,

    @field:NotBlank(message = "포지션명은 필수입니다")
    @field:Size(max = 200, message = "포지션명은 200자 이내여야 합니다")
    val positionName: String,

    @field:NotEmpty(message = "이미지는 최소 1장 이상이어야 합니다")
    val images: List<String>,

    @field:Size(max = 2000, message = "URL은 2000자 이내여야 합니다")
    val sourceUrl: String? = null
)