package com.hirelog.api.job.presentation.controller.dto.request

import com.hirelog.api.job.domain.HiringStage
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * JobSummaryReview Write Request
 *
 * 책임:
 * - 컨트롤러 입력 검증 전용
 */
data class JobSummaryReviewWriteReq(

    val hiringStage: HiringStage,

    val anonymous: Boolean,

    @field:Min(1)
    @field:Max(10)
    val difficultyRating: Int,

    @field:Min(1)
    @field:Max(10)
    val satisfactionRating: Int,

    @field:NotBlank
    val experienceComment: String,

    val interviewTip: String?
)
