package com.hirelog.api.job.presentation.controller.dto.response

import com.hirelog.api.job.domain.model.JobSummaryReview
import java.time.LocalDateTime

/**
 * JobSummaryReview Response
 *
 * 책임:
 * - 외부 노출 전용 Read Model
 */
data class JobSummaryReviewRes(
    val id: Long,
    val hiringStage: String,
    val difficultyRating: Int,
    val satisfactionRating: Int,
    val experienceComment: String,
    val interviewTip: String?,
    val anonymous: Boolean,
    val memberId: Long?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(review: JobSummaryReview): JobSummaryReviewRes =
            JobSummaryReviewRes(
                id = review.id,
                hiringStage = review.hiringStage.name,
                difficultyRating = review.difficultyRating,
                satisfactionRating = review.satisfactionRating,
                experienceComment = review.experienceComment,
                interviewTip = review.interviewTip,
                anonymous = review.isAnonymous(),
                memberId = review.getPublicMemberId(),
                createdAt = review.createdAt
            )
    }
}
