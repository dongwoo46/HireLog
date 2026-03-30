package com.hirelog.api.job.application.summary.view

import com.hirelog.api.job.domain.type.HiringStage
import java.time.LocalDateTime

/**
 * JobSummaryReview 조회용 View 모델
 */
data class JobSummaryReviewView(
    val reviewId: Long,
    val anonymous: Boolean,
    val memberId: Long?,
    val memberName: String?,
    val hiringStage: HiringStage,
    val difficultyRating: Int,
    val satisfactionRating: Int,
    val experienceComment: String,
    val interviewTip: String?,
    val deleted: Boolean,
    val createdAt: LocalDateTime
)