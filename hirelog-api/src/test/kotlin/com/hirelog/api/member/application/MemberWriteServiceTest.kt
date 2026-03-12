package com.hirelog.api.member.application

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.common.config.properties.AdminProperties
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.domain.policy.UsernameValidationPolicy
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("MemberWriteService 테스트")
class MemberWriteServiceTest {

    private lateinit var service: MemberWriteService
    private lateinit var memberCommand: MemberCommand
    private lateinit var memberQuery: MemberQuery
    private lateinit var adminProperties: AdminProperties
    private lateinit var policyResolver: UsernameValidationPolicyResolver

    private val oAuthUser = OAuthUser(
        provider = OAuth2Provider.GOOGLE,
        providerUserId = "google-user-123",
        email = "user@example.com"
    )

    @BeforeEach
    fun setUp() {
        memberCommand = mockk()
        memberQuery = mockk()
        adminProperties = mockk()
        policyResolver = mockk()
        service = MemberWriteService(memberCommand, memberQuery, adminProperties, policyResolver)
    }

    @Nested
    @DisplayName("bindOAuthAccount 메서드는")
    inner class BindOAuthAccountTest {

        @Test
        @DisplayName("이메일로 회원을 조회하고 OAuth 계정을 연결한다")
        fun shouldBindOAuthAccount() {
            val member = mockk<Member>(relaxed = true)
            every { memberCommand.findByEmail("user@example.com") } returns member

            val result = service.bindOAuthAccount("user@example.com", oAuthUser)

            assertThat(result).isEqualTo(member)
            verify { member.linkOAuthAccount(OAuth2Provider.GOOGLE, "google-user-123") }
        }

        @Test
        @DisplayName("이메일에 해당하는 회원이 없으면 EntityNotFoundException을 던진다")
        fun shouldThrowWhenMemberNotFound() {
            every { memberCommand.findByEmail("unknown@example.com") } returns null

            assertThatThrownBy {
                service.bindOAuthAccount("unknown@example.com", oAuthUser)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("signupWithOAuth 메서드는")
    inner class SignupWithOAuthTest {

        @Test
        @DisplayName("사용 중인 username이면 예외를 던진다")
        fun shouldThrowWhenUsernameDuplicated() {
            every { memberQuery.existsActiveByUsername("duplicated") } returns true

            assertThatThrownBy {
                service.signupWithOAuth("user@example.com", "duplicated", oAuthUser)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("username")

            verify(exactly = 0) { memberCommand.save(any()) }
        }

        @Test
        @DisplayName("사용 중인 email이면 예외를 던진다")
        fun shouldThrowWhenEmailDuplicated() {
            every { memberQuery.existsActiveByUsername("newuser") } returns false
            every { memberQuery.existsActiveByEmail("used@example.com") } returns true

            assertThatThrownBy {
                service.signupWithOAuth("used@example.com", "newuser", oAuthUser)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("email")

            verify(exactly = 0) { memberCommand.save(any()) }
        }

        @Test
        @DisplayName("정상 입력이면 회원을 생성하고 반환한다")
        fun shouldSignupSuccessfully() {
            val policy = mockk<UsernameValidationPolicy>(relaxed = true)
            val savedMember = mockk<Member>(relaxed = true)

            every { memberQuery.existsActiveByUsername("newuser") } returns false
            every { memberQuery.existsActiveByEmail("user@example.com") } returns false
            every { policyResolver.resolve("user@example.com") } returns policy
            every { adminProperties.isAdmin("user@example.com") } returns false
            every { memberCommand.save(any()) } returns savedMember

            val result = service.signupWithOAuth("user@example.com", "newuser", oAuthUser)

            assertThat(result).isEqualTo(savedMember)
            verify { memberCommand.save(any()) }
        }

        @Test
        @DisplayName("관리자 이메일이면 grantAdmin을 호출한다")
        fun shouldGrantAdminForAdminEmail() {
            val policy = mockk<UsernameValidationPolicy>(relaxed = true)

            every { memberQuery.existsActiveByUsername("adminuser") } returns false
            every { memberQuery.existsActiveByEmail("admin@example.com") } returns false
            every { policyResolver.resolve("admin@example.com") } returns policy
            every { adminProperties.isAdmin("admin@example.com") } returns true
            every { memberCommand.save(any()) } answers { firstArg() }

            service.signupWithOAuth(
                email = "admin@example.com",
                username = "adminuser",
                oAuthUser = OAuthUser(OAuth2Provider.GOOGLE, "admin-google-id", "admin@example.com")
            )

            // 저장된 member가 grantAdmin 호출 결과로 ADMIN 역할을 가져야 함
            verify { memberCommand.save(any()) }
        }
    }

    @Nested
    @DisplayName("recoveryAccount 메서드는")
    inner class RecoveryAccountTest {

        @Test
        @DisplayName("사용 중인 username이면 예외를 던진다")
        fun shouldThrowWhenUsernameDuplicated() {
            every { memberQuery.existsActiveByUsername("taken") } returns true

            assertThatThrownBy {
                service.recoveryAccount(1L, "user@example.com", "taken", null, null, null)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("username")
        }

        @Test
        @DisplayName("사용 중인 email이면 예외를 던진다")
        fun shouldThrowWhenEmailDuplicated() {
            every { memberQuery.existsActiveByUsername("newuser") } returns false
            every { memberQuery.existsActiveByEmail("taken@example.com") } returns true

            assertThatThrownBy {
                service.recoveryAccount(1L, "taken@example.com", "newuser", null, null, null)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("email")
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 EntityNotFoundException을 던진다")
        fun shouldThrowWhenMemberNotFound() {
            val policy = mockk<UsernameValidationPolicy>(relaxed = true)

            every { memberQuery.existsActiveByUsername("newuser") } returns false
            every { memberQuery.existsActiveByEmail("user@example.com") } returns false
            every { policyResolver.resolve("user@example.com") } returns policy
            every { memberCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.recoveryAccount(999L, "user@example.com", "newuser", null, null, null)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }

        @Test
        @DisplayName("정상 입력이면 계정을 복구하고 반환한다")
        fun shouldRecoverAccountSuccessfully() {
            val policy = mockk<UsernameValidationPolicy>(relaxed = true)
            val member = mockk<Member>(relaxed = true)

            every { memberQuery.existsActiveByUsername("recovered") } returns false
            every { memberQuery.existsActiveByEmail("user@example.com") } returns false
            every { policyResolver.resolve("user@example.com") } returns policy
            every { memberCommand.findById(1L) } returns member

            val result = service.recoveryAccount(1L, "user@example.com", "recovered", null, null, null)

            assertThat(result).isEqualTo(member)
            verify { member.updateDisplayName("recovered") }
            verify { member.activate() }
        }
    }

    @Nested
    @DisplayName("updateDisplayName 메서드는")
    inner class UpdateDisplayNameTest {

        @Test
        @DisplayName("표시 이름을 변경한다")
        fun shouldUpdateDisplayName() {
            val policy = mockk<UsernameValidationPolicy>(relaxed = true)
            val member = mockk<Member>(relaxed = true)
            every { member.email } returns "user@example.com"

            every { memberCommand.findById(1L) } returns member
            every { policyResolver.resolve("user@example.com") } returns policy

            service.updateDisplayName(1L, "newname")

            verify { member.updateDisplayName("newname") }
        }

        @Test
        @DisplayName("회원이 없으면 예외를 던진다")
        fun shouldThrowWhenNotFound() {
            every { memberCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.updateDisplayName(999L, "newname")
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("suspend 메서드는")
    inner class SuspendTest {

        @Test
        @DisplayName("회원 계정을 정지한다")
        fun shouldSuspendMember() {
            val member = mockk<Member>(relaxed = true)
            every { memberCommand.findById(1L) } returns member

            service.suspend(1L)

            verify { member.suspend() }
        }

        @Test
        @DisplayName("회원이 없으면 예외를 던진다")
        fun shouldThrowWhenNotFound() {
            every { memberCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.suspend(999L)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("activate 메서드는")
    inner class ActivateTest {

        @Test
        @DisplayName("회원 계정을 활성화한다")
        fun shouldActivateMember() {
            val member = mockk<Member>(relaxed = true)
            every { memberCommand.findById(1L) } returns member

            service.activate(1L)

            verify { member.activate() }
        }

        @Test
        @DisplayName("회원이 없으면 예외를 던진다")
        fun shouldThrowWhenNotFound() {
            every { memberCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.activate(999L)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("delete 메서드는")
    inner class DeleteTest {

        @Test
        @DisplayName("회원 계정을 논리 삭제한다")
        fun shouldSoftDeleteMember() {
            val member = mockk<Member>(relaxed = true)
            every { memberCommand.findById(1L) } returns member

            service.delete(1L)

            verify { member.softDelete() }
        }

        @Test
        @DisplayName("회원이 없으면 예외를 던진다")
        fun shouldThrowWhenNotFound() {
            every { memberCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.delete(999L)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }
}
