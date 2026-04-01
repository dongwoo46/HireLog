package com.hirelog.api.job.presentation.controller.dto.response

import com.hirelog.api.job.application.review.view.ReviewLikeStatView

data class ReviewLikeRes(
    val reviewId: Long,
    val likeCount: Long,
    val likedByMe: Boolean
) {
    companion object {
        fun from(view: ReviewLikeStatView): ReviewLikeRes =
            ReviewLikeRes(
                reviewId = view.reviewId,
                likeCount = view.likeCount,
                likedByMe = view.likedByMe
            )
    }
}
