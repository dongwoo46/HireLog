package com.hirelog.api.job.application.summary.view

import com.hirelog.api.job.domain.type.HiringStage
import java.time.LocalDateTime

/**
 * JobSummaryReview 조회용 View 모델
 */
data class JobSummaryReviewView(
    val reviewId: Long,
    val jobSummaryId: Long,
    val brandPositionName: String?,
    val anonymous: Boolean,
    val memberId: Long?,
    val memberName: String?,
    val hiringStage: HiringStage,
    val difficultyRating: Int,
    val satisfactionRating: Int,
    val prosComment: String,
    val consComment: String,
    val tip: String?,
    val likeCount: Long,
    val deleted: Boolean,
    val createdAt: LocalDateTime
)
