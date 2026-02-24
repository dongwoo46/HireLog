package com.hirelog.api.job.application.summary.view

import com.hirelog.api.job.domain.type.HiringStage
import java.time.LocalDateTime

/**
 * JobSummaryReview 조회 View
 *
 * 정책:
 * - anonymous=true → memberId, memberName 모두 null
 * - anonymous=false → memberId, memberName 포함
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
    val createdAt: LocalDateTime
)
