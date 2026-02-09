package com.hirelog.api.job.application.summary.view

import com.hirelog.api.job.domain.type.HiringStage
import java.time.LocalDateTime

/**
 * JobSummaryReview 조회 View
 */
data class JobSummaryReviewView(
    val reviewId: Long,
    val memberId: Long?,
    val hiringStage: HiringStage,
    val difficultyRating: Int,
    val satisfactionRating: Int,
    val experienceComment: String,
    val interviewTip: String?,
    val createdAt: LocalDateTime
)
