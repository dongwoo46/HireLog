package com.hirelog.api.job.presentation.controller.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class JobSummaryOcrReq(

    @field:NotBlank(message = "brandName은 필수입니다")
    @field:Size(max = 200, message = "brandName은 200자를 초과할 수 없습니다")
    val brandName: String = "",

    @field:NotBlank(message = "positionName은 필수입니다")
    @field:Size(max = 200, message = "positionName은 200자를 초과할 수 없습니다")
    val brandPositionName: String = "",

    @field:NotEmpty(message = "이미지는 최소 1개 이상 필요합니다")
    val images: List<MultipartFile> = emptyList()
)
