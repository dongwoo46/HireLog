package com.hirelog.api.job.domain.model

import com.hirelog.api.job.domain.type.HiringStage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JobSummaryReview 도메인 테스트")
class JobSummaryReviewTest {

    private fun makeReview(
        difficultyRating: Int = 5,
        satisfactionRating: Int = 7,
        prosComment: String = "좋은 경험이었습니다.",
        consComment: String = "아쉬운 점도 있었습니다.",
        tip: String? = null
    ): JobSummaryReview = JobSummaryReview.create(
        jobSummaryId = 1L,
        memberId = 100L,
        hiringStage = HiringStage.INTERVIEW_1,
        anonymous = false,
        difficultyRating = difficultyRating,
        satisfactionRating = satisfactionRating,
        prosComment = prosComment,
        consComment = consComment,
        tip = tip
    )

    @Nested
    @DisplayName("create 팩토리는")
    inner class CreateTest {

        @Test
        @DisplayName("유효한 값으로 리뷰를 생성한다")
        fun shouldCreateReview() {
            val review = makeReview()

            assertThat(review.difficultyRating).isEqualTo(5)
            assertThat(review.satisfactionRating).isEqualTo(7)
            assertThat(review.deleted).isFalse()
        }

        @Test
        @DisplayName("difficultyRating이 1 미만이면 예외를 던진다")
        fun shouldThrowWhenDifficultyRatingTooLow() {
            assertThatThrownBy { makeReview(difficultyRating = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("1~10")
        }

        @Test
        @DisplayName("difficultyRating이 10 초과이면 예외를 던진다")
        fun shouldThrowWhenDifficultyRatingTooHigh() {
            assertThatThrownBy { makeReview(difficultyRating = 11) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("satisfactionRating이 범위를 벗어나면 예외를 던진다")
        fun shouldThrowWhenSatisfactionRatingOutOfRange() {
            assertThatThrownBy { makeReview(satisfactionRating = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("prosComment가 2000자를 초과하면 예외를 던진다")
        fun shouldThrowWhenCommentTooLong() {
            assertThatThrownBy { makeReview(prosComment = "a".repeat(2001)) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("2000자")
        }

        @Test
        @DisplayName("tip이 1000자를 초과하면 예외를 던진다")
        fun shouldThrowWhenTipTooLong() {
            assertThatThrownBy { makeReview(tip = "b".repeat(1001)) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("softDelete는")
    inner class SoftDeleteTest {

        @Test
        @DisplayName("deleted를 true로 변경한다")
        fun shouldSetDeletedTrue() {
            val review = makeReview()
            review.softDelete()

            assertThat(review.isDeleted()).isTrue()
            assertThat(review.deleted).isTrue()
        }
    }

    @Nested
    @DisplayName("restore는")
    inner class RestoreTest {

        @Test
        @DisplayName("deleted를 false로 변경한다")
        fun shouldSetDeletedFalse() {
            val review = makeReview()
            review.softDelete()
            review.restore()

            assertThat(review.isDeleted()).isFalse()
        }
    }
}
