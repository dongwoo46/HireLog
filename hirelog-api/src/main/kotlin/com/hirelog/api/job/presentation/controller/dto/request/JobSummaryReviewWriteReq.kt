package com.hirelog.api.job.presentation.controller.dto.request

import com.hirelog.api.job.domain.type.HiringStage
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class JobSummaryReviewWriteReq(
    @field:NotNull(message = "hiringStage는 필수입니다")
    val hiringStage: HiringStage,

    @field:NotNull(message = "anonymous 여부는 필수입니다")
    val anonymous: Boolean,

    @field:NotNull(message = "difficultyRating은 필수입니다")
    @field:Min(value = 1, message = "difficultyRating은 1 이상이어야 합니다")
    @field:Max(value = 10, message = "difficultyRating은 10 이하여야 합니다")
    val difficultyRating: Int,

    @field:NotNull(message = "satisfactionRating은 필수입니다")
    @field:Min(value = 1, message = "satisfactionRating은 1 이상이어야 합니다")
    @field:Max(value = 10, message = "satisfactionRating은 10 이하여야 합니다")
    val satisfactionRating: Int,

    @field:NotBlank(message = "장점은 필수입니다")
    @field:Size(
        min = 10,
        max = 2000,
        message = "장점은 10자 이상 2000자 이하여야 합니다"
    )
    val prosComment: String,

    @field:NotBlank(message = "단점은 필수입니다")
    @field:Size(
        min = 10,
        max = 2000,
        message = "단점은 10자 이상 2000자 이하여야 합니다"
    )
    val consComment: String,

    @field:Size(
        max = 1000,
        message = "팁은 1000자를 초과할 수 없습니다"
    )
    val tip: String?
)
