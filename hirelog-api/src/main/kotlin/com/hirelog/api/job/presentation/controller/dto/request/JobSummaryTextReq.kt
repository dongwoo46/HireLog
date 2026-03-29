package com.hirelog.api.job.presentation.controller.dto.request

import com.hirelog.api.job.domain.type.JobPlatformType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class JobSummaryTextReq(

    @field:NotBlank(message = "brandName은 필수입니다")
    @field:Size(max = 200, message = "brandName은 200자를 초과할 수 없습니다")
    val brandName: String,

    @field:NotBlank(message = "positionName은 필수입니다")
    @field:Size(max = 200, message = "positionName은 200자를 초과할 수 없습니다")
    val brandPositionName: String,

    @field:NotBlank(message = "jdText는 비어 있을 수 없습니다")
    val jdText: String,

    @field:NotNull(message = "platform은 필수입니다")
    val platform: JobPlatformType,
)
