package com.hirelog.api.job.application.review

import com.hirelog.api.job.application.review.port.JobSummaryReviewCommand
import com.hirelog.api.job.application.review.port.ReviewLikeCommand
import com.hirelog.api.job.domain.model.JobSummaryReview
import com.hirelog.api.relation.domain.model.ReviewLike
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ReviewLikeWriteService 테스트")
class ReviewLikeWriteServiceTest {

    private lateinit var service: ReviewLikeWriteService
    private lateinit var reviewCommand: JobSummaryReviewCommand
    private lateinit var reviewLikeCommand: ReviewLikeCommand

    @BeforeEach
    fun setUp() {
        reviewCommand = mockk()
        reviewLikeCommand = mockk(relaxed = true)
        service = ReviewLikeWriteService(reviewCommand, reviewLikeCommand)
    }

    @Nested
    @DisplayName("like는")
    inner class LikeTest {

        @Test
        @DisplayName("리뷰가 없으면 예외를 던진다")
        fun throwsWhenReviewNotFound() {
            every { reviewCommand.findById(10L) } returns null

            assertThatThrownBy { service.like(reviewId = 10L, memberId = 1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("삭제된 리뷰면 예외를 던진다")
        fun throwsWhenDeletedReview() {
            val review = mockk<JobSummaryReview>()
            every { reviewCommand.findById(10L) } returns review
            every { review.isDeleted() } returns true

            assertThatThrownBy { service.like(reviewId = 10L, memberId = 1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("이미 좋아요가 있으면 저장하지 않는다")
        fun idempotentWhenAlreadyLiked() {
            val review = mockk<JobSummaryReview>()
            val like = mockk<ReviewLike>()
            every { reviewCommand.findById(10L) } returns review
            every { review.isDeleted() } returns false
            every { reviewLikeCommand.findByMemberIdAndReviewId(1L, 10L) } returns like

            service.like(reviewId = 10L, memberId = 1L)

            verify(exactly = 0) { reviewLikeCommand.save(any()) }
        }

        @Test
        @DisplayName("좋아요가 없으면 저장한다")
        fun savesWhenNotLiked() {
            val review = mockk<JobSummaryReview>()
            every { reviewCommand.findById(10L) } returns review
            every { review.isDeleted() } returns false
            every { reviewLikeCommand.findByMemberIdAndReviewId(1L, 10L) } returns null

            service.like(reviewId = 10L, memberId = 1L)

            verify(exactly = 1) { reviewLikeCommand.save(any()) }
        }
    }

    @Nested
    @DisplayName("unlike는")
    inner class UnlikeTest {

        @Test
        @DisplayName("좋아요가 없으면 아무 작업도 하지 않는다")
        fun noOpWhenNotLiked() {
            every { reviewLikeCommand.findByMemberIdAndReviewId(1L, 10L) } returns null

            service.unlike(reviewId = 10L, memberId = 1L)

            verify(exactly = 0) { reviewLikeCommand.delete(any()) }
        }

        @Test
        @DisplayName("좋아요가 있으면 삭제한다")
        fun deletesWhenLiked() {
            val like = mockk<ReviewLike>()
            every { reviewLikeCommand.findByMemberIdAndReviewId(1L, 10L) } returns like

            service.unlike(reviewId = 10L, memberId = 1L)

            verify(exactly = 1) { reviewLikeCommand.delete(like) }
        }
    }
}
