package com.hirelog.api.job.application.review

import com.hirelog.api.job.application.review.port.JobSummaryReviewCommand
import com.hirelog.api.job.domain.model.JobSummaryReview
import com.hirelog.api.job.domain.type.HiringStage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JobSummaryReviewWriteService 테스트")
class JobSummaryReviewWriteServiceTest {

    private lateinit var service: JobSummaryReviewWriteService
    private lateinit var command: JobSummaryReviewCommand

    @BeforeEach
    fun setUp() {
        command = mockk()
        service = JobSummaryReviewWriteService(command)
    }

    @Nested
    @DisplayName("write 메서드는")
    inner class WriteTest {

        @Test
        @DisplayName("해당 공고에 리뷰가 없으면 리뷰를 생성하고 반환한다")
        fun shouldCreateReviewWhenNotExists() {
            val savedReview = mockk<JobSummaryReview>(relaxed = true)

            every { command.findByJobSummaryIdAndMemberId(1L, 10L) } returns null
            every { command.save(any()) } returns savedReview

            val result = service.write(
                jobSummaryId = 1L,
                memberId = 10L,
                hiringStage = HiringStage.INTERVIEW_1,
                anonymous = false,
                difficultyRating = 3,
                satisfactionRating = 4,
                prosComment = "좋은 면접이었습니다.",
                consComment = "대기 시간이 길었습니다.",
                tip = null
            )

            assertThat(result).isEqualTo(savedReview)
            verify(exactly = 1) { command.save(any()) }
        }

        @Test
        @DisplayName("이미 리뷰가 존재하면 예외를 던진다")
        fun shouldThrowWhenReviewAlreadyExists() {
            val existing = mockk<JobSummaryReview>()
            every { command.findByJobSummaryIdAndMemberId(1L, 10L) } returns existing

            assertThatThrownBy {
                service.write(
                    jobSummaryId = 1L,
                    memberId = 10L,
                    hiringStage = HiringStage.INTERVIEW_1,
                    anonymous = false,
                    difficultyRating = 3,
                    satisfactionRating = 4,
                    prosComment = "중복 리뷰",
                    consComment = "중복 리뷰",
                    tip = null
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이미 해당 공고에 리뷰를 작성했습니다")

            verify(exactly = 0) { command.save(any()) }
        }
    }

    @Nested
    @DisplayName("delete 메서드는")
    inner class DeleteTest {

        @Test
        @DisplayName("리뷰를 Soft Delete 처리한다")
        fun shouldSoftDeleteReview() {
            val review = mockk<JobSummaryReview>(relaxed = true)
            every { review.isDeleted() } returns false

            every { command.findById(1L) } returns review
            every { command.save(review) } returns review

            service.delete(1L)

            verify { review.softDelete() }
            verify { command.save(review) }
        }

        @Test
        @DisplayName("이미 삭제된 리뷰면 예외를 던진다")
        fun shouldThrowWhenAlreadyDeleted() {
            val review = mockk<JobSummaryReview>()
            every { review.isDeleted() } returns true

            every { command.findById(1L) } returns review

            assertThatThrownBy {
                service.delete(1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이미 삭제된 리뷰")
        }

        @Test
        @DisplayName("존재하지 않는 리뷰면 예외를 던진다")
        fun shouldThrowWhenNotFound() {
            every { command.findById(999L) } returns null

            assertThatThrownBy {
                service.delete(999L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("리뷰를 찾을 수 없습니다")
        }
    }

    @Nested
    @DisplayName("restore 메서드는")
    inner class RestoreTest {

        @Test
        @DisplayName("삭제된 리뷰를 복구한다")
        fun shouldRestoreDeletedReview() {
            val review = mockk<JobSummaryReview>(relaxed = true)
            every { review.isDeleted() } returns true

            every { command.findById(1L) } returns review
            every { command.save(review) } returns review

            service.restore(1L)

            verify { review.restore() }
            verify { command.save(review) }
        }

        @Test
        @DisplayName("삭제되지 않은 리뷰는 복구할 수 없다")
        fun shouldThrowWhenNotDeleted() {
            val review = mockk<JobSummaryReview>()
            every { review.isDeleted() } returns false

            every { command.findById(1L) } returns review

            assertThatThrownBy {
                service.restore(1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("삭제되지 않은 리뷰")
        }

        @Test
        @DisplayName("존재하지 않는 리뷰는 복구할 수 없다")
        fun shouldThrowWhenNotFound() {
            every { command.findById(999L) } returns null

            assertThatThrownBy {
                service.restore(999L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("리뷰를 찾을 수 없습니다")
        }
    }
}
