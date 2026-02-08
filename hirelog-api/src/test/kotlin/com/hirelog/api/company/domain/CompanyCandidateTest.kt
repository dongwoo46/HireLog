package com.hirelog.api.company.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CompanyCandidate 도메인 테스트")
class CompanyCandidateTest {

    @Nested
    @DisplayName("CompanyCandidate 생성 테스트")
    inner class CreateTest {

        @Test
        @DisplayName("create: 초기 상태는 PENDING이며 정규화된 이름이 설정되어야 한다")
        fun create_success() {
            // given
            val jdSummaryId = 1L
            val brandId = 100L
            val candidateName = "Google Korea LLC."
            val source = CompanyCandidateSource.LLM
            val confidenceScore = 0.95

            // when
            val candidate = CompanyCandidate.create(
                jdSummaryId, brandId, candidateName, source, confidenceScore
            )

            // then
            assertEquals(jdSummaryId, candidate.jdSummaryId)
            assertEquals(brandId, candidate.brandId)
            assertEquals(candidateName, candidate.candidateName)
            assertEquals("google_korea_llc", candidate.normalizedName)
            assertEquals(source, candidate.source)
            assertEquals(confidenceScore, candidate.confidenceScore)
            assertEquals(CompanyCandidateStatus.PENDING, candidate.status)
        }
    }

    @Nested
    @DisplayName("상태 변경 테스트")
    inner class StateChangeTest {

        @Test
        @DisplayName("approve: PENDING 상태인 경우 APPROVED로 변경되어야 한다")
        fun approve_success() {
            // given
            val candidate = createCandidate()
            assertEquals(CompanyCandidateStatus.PENDING, candidate.status)

            // when
            candidate.approve()

            // then
            assertEquals(CompanyCandidateStatus.APPROVED, candidate.status)
        }

        @Test
        @DisplayName("approve: 이미 처리된(REJECTED) 상태에서는 변경되지 않아야 한다")
        fun approve_fail_when_rejected() {
            // given
            val candidate = createCandidate()
            candidate.reject()
            assertEquals(CompanyCandidateStatus.REJECTED, candidate.status)

            // when
            candidate.approve()

            // then
            assertEquals(CompanyCandidateStatus.REJECTED, candidate.status)
        }

        @Test
        @DisplayName("reject: PENDING 상태에서 REJECTED로 변경되어야 한다")
        fun reject_success() {
            // given
            val candidate = createCandidate()

            // when
            candidate.reject()

            // then
            assertEquals(CompanyCandidateStatus.REJECTED, candidate.status)
        }

        @Test
        @DisplayName("reject: 이미 REJECTED 상태여도 에러 없이 유지되어야 한다")
        fun reject_idempotent() {
            // given
            val candidate = createCandidate()
            candidate.reject()

            // when
            candidate.reject()

            // then
            assertEquals(CompanyCandidateStatus.REJECTED, candidate.status)
        }
    }

    private fun createCandidate(): CompanyCandidate {
        return CompanyCandidate.create(
            1L, 100L, "Test", CompanyCandidateSource.MANUAL, 1.0
        )
    }
}
