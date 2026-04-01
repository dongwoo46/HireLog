package com.hirelog.api.job.application.review.view

data class ReviewLikeStatView(
    val reviewId: Long,
    val likeCount: Long,
    val likedByMe: Boolean
)
