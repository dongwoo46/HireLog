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
    val prosComment: String,
    val consComment: String,
    val tip: String?,
    val likeCount: Long,
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
                prosComment = review.prosComment,
                consComment = review.consComment,
                tip = review.tip,
                likeCount = 0L,
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
                prosComment = view.prosComment,
                consComment = view.consComment,
                tip = view.tip,
                likeCount = view.likeCount,
                deleted = view.deleted,
                createdAt = view.createdAt
            )
    }
}
