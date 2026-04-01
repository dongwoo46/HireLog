package com.hirelog.api.job.application.review.port

import com.hirelog.api.job.application.review.view.ReviewLikeStatView

interface ReviewLikeQuery {
    fun findStat(reviewId: Long, memberId: Long): ReviewLikeStatView
}
