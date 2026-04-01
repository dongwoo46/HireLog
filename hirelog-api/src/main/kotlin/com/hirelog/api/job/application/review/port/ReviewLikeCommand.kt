package com.hirelog.api.job.application.review.port

import com.hirelog.api.relation.domain.model.ReviewLike

interface ReviewLikeCommand {
    fun save(reviewLike: ReviewLike): ReviewLike
    fun findByMemberIdAndReviewId(memberId: Long, reviewId: Long): ReviewLike?
    fun delete(reviewLike: ReviewLike)
}
