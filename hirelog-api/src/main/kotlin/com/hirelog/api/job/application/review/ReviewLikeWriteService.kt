package com.hirelog.api.job.application.review

import com.hirelog.api.job.application.review.port.JobSummaryReviewCommand
import com.hirelog.api.job.application.review.port.ReviewLikeCommand
import com.hirelog.api.relation.domain.model.ReviewLike
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewLikeWriteService(
    private val reviewCommand: JobSummaryReviewCommand,
    private val reviewLikeCommand: ReviewLikeCommand
) {

    @Transactional
    fun like(reviewId: Long, memberId: Long) {
        val review = reviewCommand.findById(reviewId)
            ?: throw IllegalArgumentException("리뷰를 찾을 수 없습니다: $reviewId")
        require(!review.isDeleted()) { "삭제된 리뷰에는 좋아요를 누를 수 없습니다: $reviewId" }

        val exists = reviewLikeCommand.findByMemberIdAndReviewId(memberId, reviewId)
        if (exists != null) return

        reviewLikeCommand.save(ReviewLike.create(memberId = memberId, reviewId = reviewId))
    }

    @Transactional
    fun unlike(reviewId: Long, memberId: Long) {
        val exists = reviewLikeCommand.findByMemberIdAndReviewId(memberId, reviewId) ?: return
        reviewLikeCommand.delete(exists)
    }
}
