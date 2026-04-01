package com.hirelog.api.job.application.review

import com.hirelog.api.job.application.review.port.ReviewLikeQuery
import com.hirelog.api.job.application.review.view.ReviewLikeStatView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReviewLikeReadService(
    private val reviewLikeQuery: ReviewLikeQuery
) {
    fun getStat(reviewId: Long, memberId: Long): ReviewLikeStatView =
        reviewLikeQuery.findStat(reviewId = reviewId, memberId = memberId)
}
