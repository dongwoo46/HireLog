package com.hirelog.api.relation.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.review.port.ReviewLikeQuery
import com.hirelog.api.job.application.review.view.ReviewLikeStatView
import com.hirelog.api.relation.infra.persistence.jpa.repository.ReviewLikeJpaRepository
import org.springframework.stereotype.Component

@Component
class ReviewLikeJpaQueryAdapter(
    private val repository: ReviewLikeJpaRepository
) : ReviewLikeQuery {
    override fun findStat(reviewId: Long, memberId: Long): ReviewLikeStatView {
        val row = repository.findStat(reviewId = reviewId, memberId = memberId)
        return ReviewLikeStatView(
            reviewId = reviewId,
            likeCount = row.getLikeCount(),
            likedByMe = row.getLikedByMe()
        )
    }
}
