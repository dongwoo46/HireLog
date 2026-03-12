package com.hirelog.api.company.application

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.company.application.port.CompanyCommand
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.domain.CompanySource
import com.hirelog.api.member.domain.MemberRole
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("CompanyWriteService 테스트")
class CompanyWriteServiceTest {

    private lateinit var service: CompanyWriteService
    private lateinit var companyCommand: CompanyCommand
    private lateinit var companyQuery: CompanyQuery

    private val adminMember = AuthenticatedMember(memberId = 1L, role = MemberRole.ADMIN)
    private val userMember = AuthenticatedMember(memberId = 2L, role = MemberRole.USER)

    @BeforeEach
    fun setUp() {
        companyCommand = mockk()
        companyQuery = mockk()
        service = CompanyWriteService(companyCommand, companyQuery)
        mockkObject(Normalizer)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(Normalizer)
    }

    @Nested
    @DisplayName("create 메서드는")
    inner class CreateTest {

        @Test
        @DisplayName("관리자가 새 Company를 생성하고 ID를 반환한다")
        fun shouldCreateByAdmin() {
            val name = "Toss"
            val normalizedName = "toss"
            val savedCompany = mockk<Company> { every { id } returns 1L }

            every { Normalizer.normalizeCompany(name) } returns normalizedName
            every { companyQuery.existsByNormalizedName(normalizedName) } returns false
            every { companyCommand.save(any()) } returns savedCompany

            val result = service.create(name, CompanySource.ADMIN, null, adminMember)

            assertThat(result).isEqualTo(1L)
            verify(exactly = 1) { companyCommand.save(any()) }
        }

        @Test
        @DisplayName("관리자가 아니면 생성을 거부한다")
        fun shouldRejectNonAdmin() {
            assertThatThrownBy {
                service.create("Toss", CompanySource.ADMIN, null, userMember)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ADMIN")

            verify(exactly = 0) { companyCommand.save(any()) }
        }

        @Test
        @DisplayName("normalizedName이 빈 값이면 예외를 던진다")
        fun shouldThrowWhenNormalizedNameBlank() {
            val name = "---"
            every { Normalizer.normalizeCompany(name) } returns ""

            assertThatThrownBy {
                service.create(name, CompanySource.ADMIN, null, adminMember)
            }.isInstanceOf(IllegalArgumentException::class.java)

            verify(exactly = 0) { companyCommand.save(any()) }
        }

        @Test
        @DisplayName("이미 존재하는 normalizedName이면 예외를 던진다")
        fun shouldThrowWhenDuplicate() {
            val name = "Toss"
            val normalizedName = "toss"

            every { Normalizer.normalizeCompany(name) } returns normalizedName
            every { companyQuery.existsByNormalizedName(normalizedName) } returns true

            assertThatThrownBy {
                service.create(name, CompanySource.ADMIN, null, adminMember)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("already exists")

            verify(exactly = 0) { companyCommand.save(any()) }
        }

        @Test
        @DisplayName("동시성 충돌 시 IllegalStateException을 던진다")
        fun shouldThrowOnConcurrentConflict() {
            val name = "Toss"
            val normalizedName = "toss"

            every { Normalizer.normalizeCompany(name) } returns normalizedName
            every { companyQuery.existsByNormalizedName(normalizedName) } returns false
            every { companyCommand.save(any()) } throws DataIntegrityViolationException("Duplicate key")

            assertThatThrownBy {
                service.create(name, CompanySource.ADMIN, null, adminMember)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("already exists")
        }
    }

    @Nested
    @DisplayName("changeName 메서드는")
    inner class ChangeNameTest {

        @Test
        @DisplayName("Company 이름을 변경한다")
        fun shouldChangeName() {
            val company = mockk<Company>(relaxed = true)

            every { companyCommand.findById(1L) } returns company
            every { companyCommand.save(company) } returns company

            service.changeName(1L, "New Name")

            verify { company.changeName("New Name") }
            verify { companyCommand.save(company) }
        }

        @Test
        @DisplayName("존재하지 않는 Company는 변경할 수 없다")
        fun shouldThrowWhenNotFound() {
            every { companyCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.changeName(999L, "New Name")
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("activate 메서드는")
    inner class ActivateTest {

        @Test
        @DisplayName("관리자가 Company를 활성화한다")
        fun shouldActivateByAdmin() {
            val company = mockk<Company>(relaxed = true)

            every { companyCommand.findById(1L) } returns company
            every { companyCommand.save(company) } returns company

            service.activate(1L, adminMember)

            verify { company.activate() }
            verify { companyCommand.save(company) }
        }

        @Test
        @DisplayName("관리자가 아니면 활성화를 거부한다")
        fun shouldRejectNonAdmin() {
            assertThatThrownBy {
                service.activate(1L, userMember)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ADMIN")
        }

        @Test
        @DisplayName("존재하지 않는 Company는 활성화할 수 없다")
        fun shouldThrowWhenNotFound() {
            every { companyCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.activate(999L, adminMember)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deactivate 메서드는")
    inner class DeactivateTest {

        @Test
        @DisplayName("관리자가 Company를 비활성화한다")
        fun shouldDeactivateByAdmin() {
            val company = mockk<Company>(relaxed = true)

            every { companyCommand.findById(1L) } returns company
            every { companyCommand.save(company) } returns company

            service.deactivate(1L, adminMember)

            verify { company.deactivate() }
            verify { companyCommand.save(company) }
        }

        @Test
        @DisplayName("관리자가 아니면 비활성화를 거부한다")
        fun shouldRejectNonAdmin() {
            assertThatThrownBy {
                service.deactivate(1L, userMember)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ADMIN")
        }

        @Test
        @DisplayName("존재하지 않는 Company는 비활성화할 수 없다")
        fun shouldThrowWhenNotFound() {
            every { companyCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.deactivate(999L, adminMember)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }
}
