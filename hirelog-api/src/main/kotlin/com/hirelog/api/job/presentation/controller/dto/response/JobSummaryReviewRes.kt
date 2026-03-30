package com.hirelog.api.job.presentation.controller.dto.response

import com.hirelog.api.job.application.summary.view.JobSummaryReviewView
import com.hirelog.api.job.domain.model.JobSummaryReview
import java.time.LocalDateTime

/**
 * JobSummaryReview API 응답 모델
 */
data class JobSummaryReviewRes(
    val id: Long,
    val anonymous: Boolean,
    val memberId: Long?,
    val memberName: String?,
    val hiringStage: String,
    val difficultyRating: Int,
    val satisfactionRating: Int,
    val experienceComment: String,
    val interviewTip: String?,
    val deleted: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(review: JobSummaryReview): JobSummaryReviewRes =
            JobSummaryReviewRes(
                id = review.id,
                anonymous = review.anonymous,
                memberId = if (review.anonymous) null else review.memberId,
                memberName = null,
                hiringStage = review.hiringStage.name,
                difficultyRating = review.difficultyRating,
                satisfactionRating = review.satisfactionRating,
                experienceComment = review.experienceComment,
                interviewTip = review.interviewTip,
                deleted = review.deleted,
                createdAt = review.createdAt
            )

        fun from(view: JobSummaryReviewView): JobSummaryReviewRes =
            JobSummaryReviewRes(
                id = view.reviewId,
                anonymous = view.anonymous,
                memberId = view.memberId,
                memberName = view.memberName,
                hiringStage = view.hiringStage.name,
                difficultyRating = view.difficultyRating,
                satisfactionRating = view.satisfactionRating,
                experienceComment = view.experienceComment,
                interviewTip = view.interviewTip,
                deleted = view.deleted,
                createdAt = view.createdAt
            )
    }
}