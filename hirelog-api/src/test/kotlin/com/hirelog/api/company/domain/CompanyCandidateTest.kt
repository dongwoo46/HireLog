package com.hirelog.api.company.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("CompanyCandidate 도메인 테스트")
class CompanyCandidateTest {

    private fun makeCandidate(candidateName: String = "(주)비바리퍼블리카"): CompanyCandidate =
        CompanyCandidate.create(
            jdSummaryId = 1L,
            brandId = 10L,
            candidateName = candidateName,
            source = CompanyCandidateSource.LLM,
            confidenceScore = 0.9
        )

    @Nested
    @DisplayName("create 팩토리는")
    inner class CreateTest {

        @Test
        @DisplayName("PENDING 상태로 생성하고 normalizedName을 자동 계산한다")
        fun shouldCreateWithPendingStatus() {
            val candidate = makeCandidate("(주)비바리퍼블리카")

            assertThat(candidate.status).isEqualTo(CompanyCandidateStatus.PENDING)
            assertThat(candidate.candidateName).isEqualTo("(주)비바리퍼블리카")
            assertThat(candidate.normalizedName).isNotBlank()
        }

        @Test
        @DisplayName("법인 접미사가 제거된 normalizedName을 반환한다")
        fun shouldNormalizeCompanySuffix() {
            assertThat(makeCandidate("(주)비바리퍼블리카").normalizedName).isEqualTo("비바리퍼블리카")
            assertThat(makeCandidate("주식회사 카카오").normalizedName).isEqualTo("카카오")
            assertThat(makeCandidate("Kakao Corp").normalizedName).isEqualTo("kakao")
        }
    }

    @Nested
    @DisplayName("approve는")
    inner class ApproveTest {

        @Test
        @DisplayName("PENDING → APPROVED로 전이한다")
        fun shouldApprove() {
            val candidate = makeCandidate()
            candidate.approve()
            assertThat(candidate.status).isEqualTo(CompanyCandidateStatus.APPROVED)
        }

        @Test
        @DisplayName("PENDING 이외 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotPending() {
            val candidate = makeCandidate()
            candidate.approve()

            assertThatThrownBy { candidate.approve() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING")
        }
    }

    @Nested
    @DisplayName("reject는")
    inner class RejectTest {

        @Test
        @DisplayName("PENDING → REJECTED로 전이한다")
        fun shouldReject() {
            val candidate = makeCandidate()
            candidate.reject()
            assertThat(candidate.status).isEqualTo(CompanyCandidateStatus.REJECTED)
        }

        @Test
        @DisplayName("APPROVED 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotPending() {
            val candidate = makeCandidate()
            candidate.approve()

            assertThatThrownBy { candidate.reject() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("markProcessing는")
    inner class MarkProcessingTest {

        @Test
        @DisplayName("APPROVED → PROCESSING으로 전이한다")
        fun shouldMarkProcessing() {
            val candidate = makeCandidate()
            candidate.approve()
            candidate.markProcessing()
            assertThat(candidate.status).isEqualTo(CompanyCandidateStatus.PROCESSING)
        }

        @Test
        @DisplayName("PENDING 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotApproved() {
            val candidate = makeCandidate()

            assertThatThrownBy { candidate.markProcessing() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("APPROVED")
        }
    }

    @Nested
    @DisplayName("complete는")
    inner class CompleteTest {

        @Test
        @DisplayName("PROCESSING → COMPLETED로 전이한다")
        fun shouldComplete() {
            val candidate = makeCandidate()
            candidate.approve()
            candidate.markProcessing()
            candidate.complete()
            assertThat(candidate.status).isEqualTo(CompanyCandidateStatus.COMPLETED)
        }

        @Test
        @DisplayName("PROCESSING 이외 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotProcessing() {
            val candidate = makeCandidate()
            candidate.approve()

            assertThatThrownBy { candidate.complete() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PROCESSING")
        }
    }

    @Nested
    @DisplayName("fail은")
    inner class FailTest {

        @Test
        @DisplayName("PROCESSING → FAILED로 전이한다")
        fun shouldFail() {
            val candidate = makeCandidate()
            candidate.approve()
            candidate.markProcessing()
            candidate.fail()
            assertThat(candidate.status).isEqualTo(CompanyCandidateStatus.FAILED)
        }
    }

    @Nested
    @DisplayName("retry는")
    inner class RetryTest {

        @Test
        @DisplayName("FAILED → APPROVED로 전이한다")
        fun shouldRetry() {
            val candidate = makeCandidate()
            candidate.approve()
            candidate.markProcessing()
            candidate.fail()
            candidate.retry()
            assertThat(candidate.status).isEqualTo(CompanyCandidateStatus.APPROVED)
        }

        @Test
        @DisplayName("FAILED 이외 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotFailed() {
            val candidate = makeCandidate()

            assertThatThrownBy { candidate.retry() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("FAILED")
        }
    }
}
