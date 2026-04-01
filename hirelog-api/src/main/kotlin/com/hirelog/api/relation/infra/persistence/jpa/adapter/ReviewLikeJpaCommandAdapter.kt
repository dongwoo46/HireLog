package com.hirelog.api.relation.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.review.port.ReviewLikeCommand
import com.hirelog.api.relation.domain.model.ReviewLike
import com.hirelog.api.relation.infra.persistence.jpa.repository.ReviewLikeJpaRepository
import org.springframework.stereotype.Component

@Component
class ReviewLikeJpaCommandAdapter(
    private val repository: ReviewLikeJpaRepository
) : ReviewLikeCommand {
    override fun save(reviewLike: ReviewLike): ReviewLike = repository.save(reviewLike)

    override fun findByMemberIdAndReviewId(memberId: Long, reviewId: Long): ReviewLike? =
        repository.findByMemberIdAndReviewId(memberId, reviewId)

    override fun delete(reviewLike: ReviewLike) {
        repository.delete(reviewLike)
    }
}
