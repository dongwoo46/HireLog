package com.hirelog.api.company.application

import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.company.application.port.CompanyCandidateCommand
import com.hirelog.api.company.application.port.CompanyCandidateQuery
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.CompanyCandidateSource
import io.mockk.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("CompanyCandidateWriteService 테스트")
class CompanyCandidateWriteServiceTest {

    private lateinit var service: CompanyCandidateWriteService
    private lateinit var command: CompanyCandidateCommand
    private lateinit var query: CompanyCandidateQuery

    @BeforeEach
    fun setUp() {
        command = mockk()
        query = mockk()
        service = CompanyCandidateWriteService(command, query)
        mockkObject(Normalizer)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(Normalizer)
    }

    @Nested
    @DisplayName("createCandidate 메서드는")
    inner class CreateCandidateTest {

        @Test
        @DisplayName("중복이 없으면 후보를 생성한다")
        fun shouldCreateSuccessfully() {
            val brandId = 1L
            val candidateName = "Toss"
            val normalizedName = "toss"

            every { Normalizer.normalizeCompany(candidateName) } returns normalizedName
            every { query.existsByBrandIdAndNormalizedName(brandId, normalizedName) } returns false
            every { command.save(any()) } returns mockk()

            service.createCandidate(10L, brandId, candidateName, CompanyCandidateSource.LLM, 0.9)

            verify(exactly = 1) { command.save(any()) }
        }

        @Test
        @DisplayName("동일 Brand + normalizedName이 존재하면 예외를 던진다")
        fun shouldThrowWhenDuplicate() {
            val brandId = 1L
            val candidateName = "Toss"
            val normalizedName = "toss"

            every { Normalizer.normalizeCompany(candidateName) } returns normalizedName
            every { query.existsByBrandIdAndNormalizedName(brandId, normalizedName) } returns true

            assertThatThrownBy {
                service.createCandidate(10L, brandId, candidateName, CompanyCandidateSource.LLM, 0.9)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("already exists")

            verify(exactly = 0) { command.save(any()) }
        }

        @Test
        @DisplayName("동시성 충돌(DataIntegrityViolationException) 발생 시 IllegalStateException을 던진다")
        fun shouldThrowOnConcurrentConflict() {
            val brandId = 1L
            val candidateName = "Toss"
            val normalizedName = "toss"

            every { Normalizer.normalizeCompany(candidateName) } returns normalizedName
            every { query.existsByBrandIdAndNormalizedName(brandId, normalizedName) } returns false
            every { command.save(any()) } throws DataIntegrityViolationException("Duplicate key")

            assertThatThrownBy {
                service.createCandidate(10L, brandId, candidateName, CompanyCandidateSource.LLM, 0.9)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("concurrent")
        }
    }

    @Nested
    @DisplayName("approve 메서드는")
    inner class ApproveTest {

        @Test
        @DisplayName("후보를 승인 상태로 변경한다")
        fun shouldApproveCandidate() {
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { command.findByIdForUpdate(1L) } returns candidate

            service.approve(1L)

            verify { candidate.approve() }
        }

        @Test
        @DisplayName("존재하지 않는 후보 승인 시 예외를 던진다")
        fun shouldThrowWhenCandidateNotFound() {
            every { command.findByIdForUpdate(999L) } returns null

            assertThatThrownBy {
                service.approve(999L)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("not found")
        }
    }

    @Nested
    @DisplayName("reject 메서드는")
    inner class RejectTest {

        @Test
        @DisplayName("후보를 거절 상태로 변경한다")
        fun shouldRejectCandidate() {
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { command.findByIdForUpdate(1L) } returns candidate

            service.reject(1L)

            verify { candidate.reject() }
        }

        @Test
        @DisplayName("존재하지 않는 후보 거절 시 예외를 던진다")
        fun shouldThrowWhenCandidateNotFound() {
            every { command.findByIdForUpdate(999L) } returns null

            assertThatThrownBy {
                service.reject(999L)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("not found")
        }
    }
}
