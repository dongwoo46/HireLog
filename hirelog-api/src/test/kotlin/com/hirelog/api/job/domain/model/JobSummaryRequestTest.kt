package com.hirelog.api.job.domain.model

import com.hirelog.api.job.domain.type.JobSummaryRequestStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("JobSummaryRequest 도메인 테스트")
class JobSummaryRequestTest {

    @Nested
    @DisplayName("create 팩토리는")
    inner class CreateTest {

        @Test
        @DisplayName("정상 값으로 PENDING 상태의 Request를 생성한다")
        fun shouldCreateWithPendingStatus() {
            val request = JobSummaryRequest.create(memberId = 1L, requestId = "req-001")

            assertThat(request.memberId).isEqualTo(1L)
            assertThat(request.requestId).isEqualTo("req-001")
            assertThat(request.status).isEqualTo(JobSummaryRequestStatus.PENDING)
            assertThat(request.jobSummaryId).isNull()
        }

        @Test
        @DisplayName("memberId가 0 이하이면 예외를 던진다")
        fun shouldThrowWhenInvalidMemberId() {
            assertThatThrownBy { JobSummaryRequest.create(memberId = 0L, requestId = "req-001") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("blank requestId이면 예외를 던진다")
        fun shouldThrowWhenBlankRequestId() {
            assertThatThrownBy { JobSummaryRequest.create(memberId = 1L, requestId = "") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("complete는")
    inner class CompleteTest {

        @Test
        @DisplayName("PENDING → COMPLETED로 전이하고 jobSummaryId를 설정한다")
        fun shouldTransitionToCompleted() {
            val request = JobSummaryRequest.create(memberId = 1L, requestId = "req-001")
            request.complete(jobSummaryId = 99L)

            assertThat(request.status).isEqualTo(JobSummaryRequestStatus.COMPLETED)
            assertThat(request.jobSummaryId).isEqualTo(99L)
        }

        @Test
        @DisplayName("jobSummaryId가 0 이하이면 예외를 던진다")
        fun shouldThrowWhenInvalidJobSummaryId() {
            val request = JobSummaryRequest.create(memberId = 1L, requestId = "req-001")

            assertThatThrownBy { request.complete(jobSummaryId = 0L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("PENDING 이외 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotPending() {
            val request = JobSummaryRequest.create(memberId = 1L, requestId = "req-001")
            request.markFailed()

            assertThatThrownBy { request.complete(jobSummaryId = 1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("FAILED")
        }
    }

    @Nested
    @DisplayName("markFailed는")
    inner class MarkFailedTest {

        @Test
        @DisplayName("PENDING → FAILED로 전이한다")
        fun shouldTransitionToFailed() {
            val request = JobSummaryRequest.create(memberId = 1L, requestId = "req-001")
            request.markFailed()

            assertThat(request.status).isEqualTo(JobSummaryRequestStatus.FAILED)
        }

        @Test
        @DisplayName("PENDING 이외 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotPending() {
            val request = JobSummaryRequest.create(memberId = 1L, requestId = "req-001")
            request.complete(jobSummaryId = 1L)

            assertThatThrownBy { request.markFailed() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("COMPLETED")
        }
    }
}
