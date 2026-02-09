package com.hirelog.api.company.application

import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.company.application.port.CompanyCommand
import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.domain.CompanySource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("CompanyWriteService 테스트")
class CompanyWriteServiceTest {

    private val companyCommand: CompanyCommand = mockk()
    private val service = CompanyWriteService(companyCommand)

    @Nested
    @DisplayName("getOrCreate 테스트")
    inner class GetOrCreateTest {

        @Test
        @DisplayName("이미 존재하는 회사는 저장 없이 반환되어야 한다")
        fun return_existing_company() {
            // given
            val existingCompany = mockk<Company>()
            every { companyCommand.findByNormalizedName("toss") } returns existingCompany

            // when
            val result = service.getOrCreate("Toss", emptyList(), CompanySource.ADMIN, null)

            // then
            assertEquals(existingCompany, result)
            verify(exactly = 0) { companyCommand.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 회사는 새로 생성하여 저장해야 한다")
        fun create_new_company() {
            // given
            val newCompany = mockk<Company>()
            every { companyCommand.findByNormalizedName("new_co") } returns null
            every { companyCommand.save(any()) } returns newCompany

            // when
            val result = service.getOrCreate("New Co", emptyList(), CompanySource.ADMIN, null)

            // then
            assertEquals(newCompany, result)
            verify(exactly = 1) { companyCommand.save(any()) }
        }

        @Test
        @DisplayName("동시성 충돌 시 재조회하여 반환해야 한다")
        fun handle_concurrency_conflict() {
            // given
            val existing = mockk<Company>()
            
            // 1st check: null
            every { companyCommand.findByNormalizedName("concurrent") } returnsMany listOf(null, existing)
            // Save: throws conflict
            every { companyCommand.save(any()) } throws DataIntegrityViolationException("Dup")
            
            // when
            val result = service.getOrCreate("Concurrent", emptyList(), CompanySource.ADMIN, null)
            
            // then
            assertEquals(existing, result)
            verify(exactly = 2) { companyCommand.findByNormalizedName("concurrent") }
        }
    }

    @Nested
    @DisplayName("verify 테스트")
    inner class VerifyTest {

        @Test
        @DisplayName("성공적으로 verify를 호출해야 한다")
        fun success() {
            // given
            val company = mockk<Company>(relaxed = true)
            every { companyCommand.findById(1L) } returns company

            // when
            service.verify(1L)

            // then
            verify(exactly = 1) { company.verify() }
        }

        @Test
        @DisplayName("회사가 없으면 예외 발생")
        fun not_found() {
            // given
            every { companyCommand.findById(999L) } returns null

            // when & then
            assertThrows(EntityNotFoundException::class.java) {
                service.verify(999L)
            }
        }
    }

    @Nested
    @DisplayName("deactivate 테스트")
    inner class DeactivateTest {

        @Test
        @DisplayName("성공적으로 deactivate를 호출해야 한다")
        fun success() {
            // given
            val company = mockk<Company>(relaxed = true)
            every { companyCommand.findById(1L) } returns company

            // when
            service.deactivate(1L)

            // then
            verify(exactly = 1) { company.deactivate() }
        }
    }
}
