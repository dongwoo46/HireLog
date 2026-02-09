package com.hirelog.api.company.application

import com.hirelog.api.company.application.port.CompanyCandidateCommand
import com.hirelog.api.company.application.port.CompanyCandidateQuery
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.CompanyCandidateSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("CompanyCandidateWriteService 테스트")
class CompanyCandidateWriteServiceTest {

    private val command: CompanyCandidateCommand = mockk()
    private val query: CompanyCandidateQuery = mockk()
    
    private val service = CompanyCandidateWriteService(command, query)

    @Nested
    @DisplayName("createCandidate 테스트")
    inner class CreateCandidateTest {

        @Test
        @DisplayName("이미 존재하는 후보면 에러 발생")
        fun error_if_exists() {
            // given
            every { query.existsByBrandIdAndNormalizedName(any(), any()) } returns true
            
            // when & then
            val ex = assertThrows(IllegalStateException::class.java) {
                service.createCandidate(1L, 1L, "Test", CompanyCandidateSource.MANUAL, 0.5)
            }
            assertEquals("CompanyCandidate already exists. brandId=1, name=Test", ex.message)
        }

        @Test
        @DisplayName("존재하지 않으면 새로 저장")
        fun success_create() {
            // given
            every { query.existsByBrandIdAndNormalizedName(any(), any()) } returns false
            every { command.save(any()) } answers { firstArg() }

            // when
            val result = service.createCandidate(1L, 1L, "New", CompanyCandidateSource.MANUAL, 0.9)

            // then
            assertEquals("New", result.candidateName)
            verify(exactly = 1) { command.save(any()) }
        }

        @Test
        @DisplayName("동시성 충돌 시 재조회")
        fun concurrency_conflict() {
            // given
            val existing = mockk<CompanyCandidate>()
            every { query.existsByBrandIdAndNormalizedName(any(), any()) } returns false
            
            // save 실패 -> find 호출
            every { command.save(any()) } throws DataIntegrityViolationException("Conflict")
            every { command.findByNormalizedName(any()) } returns existing

            // when
            val result = service.createCandidate(1L, 1L, "Concurrent", CompanyCandidateSource.MANUAL, 0.9)

            // then
            assertEquals(existing, result)
        }
    }

    @Nested
    @DisplayName("approve / reject 테스트")
    inner class StatusChangeTest {

        @Test
        @DisplayName("approve: 후보를 로드하고 승인 처리")
        fun approve() {
            // given
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { command.findByIdForUpdate(1L) } returns candidate

            // when
            service.approve(1L)

            // then
            verify(exactly = 1) { candidate.approve() }
        }

        @Test
        @DisplayName("reject: 후보를 로드하고 거절 처리")
        fun reject() {
            // given
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { command.findByIdForUpdate(2L) } returns candidate

            // when
            service.reject(2L)

            // then
            verify(exactly = 1) { candidate.reject() }
        }

        @Test
        @DisplayName("존재하지 않는 후보 처리 시 에러")
        fun not_found() {
            // given
            every { command.findByIdForUpdate(999L) } returns null

            // when & then
            val ex = assertThrows(IllegalStateException::class.java) {
                service.approve(999L)
            }
            assertEquals("CompanyCandidate not found. id=999", ex.message)
        }
    }
}
