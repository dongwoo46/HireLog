package com.hirelog.api.member.application

import com.hirelog.api.auth.application.TokenService
import com.hirelog.api.auth.application.dto.AuthTokens
import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.member.application.dto.SignupResult
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.domain.MemberRole
import com.hirelog.api.member.presentation.dto.BindRequest
import com.hirelog.api.member.presentation.dto.CheckEmailResponse
import com.hirelog.api.member.presentation.dto.SignupCompleteRequest
import com.hirelog.api.member.presentation.dto.VerifyCodeResponse
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("회원가입 Facade")
class SignupFacadeServiceTest {

    private val sessionService: SignupSessionService = mockk()
    private val emailVerificationService: EmailVerificationService = mockk()
    private val memberQuery: MemberQuery = mockk()
    private val memberWriteService: MemberWriteService = mockk()
    private val tokenService: TokenService = mockk()

    private lateinit var facade: SignupFacadeService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        facade = SignupFacadeService(
            sessionService = sessionService,
            emailVerificationService = emailVerificationService,
            memberQuery = memberQuery,
            memberWriteService = memberWriteService,
            tokenService = tokenService
        )
    }

    /* =========================
     * Test Fixtures
     * ========================= */

    private fun createOAuthUser(
        provider: OAuth2Provider = OAuth2Provider.GOOGLE,
        providerUserId: String = "oauth-123",
        email: String? = "test@example.com"
    ) = OAuthUser(
        provider = provider,
        providerUserId = providerUserId,
        email = email
    )

    private fun createMember(
        id: Long = 100L,
        email: String = "test@example.com",
        role: MemberRole = MemberRole.USER
    ) = mockk<Member> {
        every { this@mockk.id } returns id
        every { this@mockk.email } returns email
        every { this@mockk.role } returns role
    }

    private fun createAuthTokens(
        accessToken: String = "access-token-123",
        refreshToken: String = "refresh-token-456"
    ) = AuthTokens(
        accessToken = accessToken,
        refreshToken = refreshToken
    )

    private fun createSignupCompleteRequest(
        email: String = "test@example.com",
        username: String = "testuser",
        currentPositionId: Long? = null,
        careerYears: Int? = null,
        summary: String? = null
    ) = SignupCompleteRequest(
        email = email,
        username = username,
        currentPositionId = currentPositionId,
        careerYears = careerYears,
        summary = summary
    )

    private fun createBindRequest(
        email: String = "test@example.com"
    ) = BindRequest(email = email)

    /* =========================
     * 이메일 중복 체크
     * ========================= */

    @Nested
    @DisplayName("이메일 중복 체크")
    inner class CheckEmailAvailability {

        private val signupToken = "valid-signup-token"
        private val email = "test@example.com"

        @Test
        fun `신규 이메일이면 인증코드 발송하고 exists=false 반환`() {
            // Given
            every { sessionService.validate(signupToken) } just Runs
            every { memberQuery.existsByEmail(email) } returns false
            every { emailVerificationService.generateAndSave(signupToken, email) } returns "123456"

            // When
            val response = facade.checkEmailAvailability(signupToken, email)

            // Then
            assertFalse(response.exists)
            verify(exactly = 1) { emailVerificationService.generateAndSave(signupToken, email) }
        }

        @Test
        fun `기존 이메일이면 인증코드 발송 안하고 exists=true 반환`() {
            // Given
            every { sessionService.validate(signupToken) } just Runs
            every { memberQuery.existsByEmail(email) } returns true

            // When
            val response = facade.checkEmailAvailability(signupToken, email)

            // Then
            assertTrue(response.exists)
            verify(exactly = 0) { emailVerificationService.generateAndSave(any(), any()) }
        }

        @Test
        fun `유효하지 않은 signupToken이면 예외 발생`() {
            // Given
            every { sessionService.validate(signupToken) } throws IllegalArgumentException("Invalid token")

            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                facade.checkEmailAvailability(signupToken, email)
            }
            verify(exactly = 0) { memberQuery.existsByEmail(any()) }
        }
    }

    /* =========================
     * 인증코드 발송 (기존 계정 연결)
     * ========================= */

    @Nested
    @DisplayName("인증코드 발송")
    inner class SendVerificationCode {

        private val signupToken = "valid-token"
        private val email = "existing@example.com"

        @Test
        fun `기존 계정이 있으면 인증코드 발송`() {
            // Given
            every { sessionService.validate(signupToken) } just Runs
            every { memberQuery.existsByEmail(email) } returns true
            every { emailVerificationService.generateAndSave(signupToken, email) } returns "123456"

            // When
            facade.sendVerificationCode(signupToken, email)

            // Then
            verify(exactly = 1) { emailVerificationService.generateAndSave(signupToken, email) }
        }

        @Test
        fun `존재하지 않는 이메일이면 예외 발생`() {
            // Given
            every { sessionService.validate(signupToken) } just Runs
            every { memberQuery.existsByEmail(email) } returns false

            // When & Then
            val exception = assertThrows(IllegalArgumentException::class.java) {
                facade.sendVerificationCode(signupToken, email)
            }
            assertEquals("존재하지 않는 이메일입니다.", exception.message)
            verify(exactly = 0) { emailVerificationService.generateAndSave(any(), any()) }
        }
    }

    /* =========================
     * 인증코드 검증
     * ========================= */

    @Nested
    @DisplayName("인증코드 검증")
    inner class VerifyCode {

        private val signupToken = "valid-token"
        private val email = "test@example.com"
        private val code = "123456"

        @Test
        fun `올바른 인증코드면 verified=true 반환`() {
            // Given
            every { sessionService.validate(signupToken) } just Runs
            every { emailVerificationService.verify(signupToken, email, code) } returns true

            // When
            val response = facade.verifyCode(signupToken, email, code)

            // Then
            assertTrue(response.verified)
        }

        @Test
        fun `잘못된 인증코드면 verified=false 반환`() {
            // Given
            every { sessionService.validate(signupToken) } just Runs
            every { emailVerificationService.verify(signupToken, email, code) } returns false

            // When
            val response = facade.verifyCode(signupToken, email, code)

            // Then
            assertFalse(response.verified)
        }
    }

    /* =========================
     * 기존 계정 연동
     * ========================= */

    @Nested
    @DisplayName("기존 계정 연동")
    inner class BindOAuthAccount {

        private val signupToken = "valid-token"
        private val email = "existing@example.com"

        @Test
        fun `이메일 인증 완료 후 계정 연동 성공`() {
            // Given
            val request = createBindRequest(email)
            val oAuthUser = createOAuthUser(email = email)
            val member = createMember(id = 200L, email = email)
            val tokens = createAuthTokens()

            every { emailVerificationService.isVerified(signupToken, email) } returns true
            every { sessionService.getOAuthUser(signupToken) } returns oAuthUser
            every { memberWriteService.bindOAuthAccount(email, oAuthUser) } returns member
            every { tokenService.generateAuthTokens(200L, "USER") } returns tokens
            every { sessionService.clear(signupToken) } just Runs
            every { emailVerificationService.clearVerified(signupToken) } just Runs

            // When
            val result = facade.bindOAuthAccount(signupToken, request)

            // Then
            assertEquals(200L, result.memberId)
            assertEquals("access-token-123", result.accessToken)
            assertEquals("refresh-token-456", result.refreshToken)

            verify(exactly = 1) { memberWriteService.bindOAuthAccount(email, oAuthUser) }
            verify(exactly = 1) { sessionService.clear(signupToken) }
            verify(exactly = 1) { emailVerificationService.clearVerified(signupToken) }
        }

        @Test
        fun `이메일 인증 안되었으면 예외 발생`() {
            // Given
            val request = createBindRequest(email)
            every { emailVerificationService.isVerified(signupToken, email) } returns false

            // When & Then
            val exception = assertThrows(IllegalArgumentException::class.java) {
                facade.bindOAuthAccount(signupToken, request)
            }
            assertEquals("이메일 인증이 완료되지 않았습니다.", exception.message)

            verify(exactly = 0) { memberWriteService.bindOAuthAccount(any(), any()) }
            verify(exactly = 0) { sessionService.clear(any()) }
        }

        @Test
        fun `계정 연동 성공 시 세션 정리`() {
            // Given
            val request = createBindRequest(email)
            val oAuthUser = createOAuthUser()
            val member = createMember()
            val tokens = createAuthTokens()

            every { emailVerificationService.isVerified(signupToken, email) } returns true
            every { sessionService.getOAuthUser(signupToken) } returns oAuthUser
            every { memberWriteService.bindOAuthAccount(any(), any()) } returns member
            every { tokenService.generateAuthTokens(any(), any()) } returns tokens
            every { sessionService.clear(signupToken) } just Runs
            every { emailVerificationService.clearVerified(signupToken) } just Runs

            // When
            facade.bindOAuthAccount(signupToken, request)

            // Then - 세션 정리 반드시 호출
            verify(exactly = 1) { sessionService.clear(signupToken) }
            verify(exactly = 1) { emailVerificationService.clearVerified(signupToken) }
        }
    }

    /* =========================
     * 신규 회원가입
     * ========================= */

    @Nested
    @DisplayName("신규 회원가입 완료")
    inner class CompleteSignup {

        private val signupToken = "valid-token"
        private val email = "newuser@example.com"

        @Test
        fun `이메일 인증 완료 후 회원가입 성공`() {
            // Given
            val request = createSignupCompleteRequest(
                email = email,
                username = "newuser",
                currentPositionId = 5L,
                careerYears = 3,
                summary = "백엔드 개발자"
            )
            val oAuthUser = createOAuthUser(email = email)
            val member = createMember(id = 100L, email = email)
            val tokens = createAuthTokens()

            every { emailVerificationService.isVerified(signupToken, email) } returns true
            every { sessionService.getOAuthUser(signupToken) } returns oAuthUser
            every {
                memberWriteService.signupWithOAuth(
                    email = email,
                    username = "newuser",
                    oAuthUser = oAuthUser,
                    currentPositionId = 5L,
                    careerYears = 3,
                    summary = "백엔드 개발자"
                )
            } returns member
            every { tokenService.generateAuthTokens(100L, "USER") } returns tokens
            every { sessionService.clear(signupToken) } just Runs
            every { emailVerificationService.clearVerified(signupToken) } just Runs

            // When
            val result = facade.completeSignup(signupToken, request)

            // Then
            assertEquals(100L, result.memberId)
            assertEquals("access-token-123", result.accessToken)
            assertEquals("refresh-token-456", result.refreshToken)

            verify(exactly = 1) {
                memberWriteService.signupWithOAuth(
                    email = email,
                    username = "newuser",
                    oAuthUser = oAuthUser,
                    currentPositionId = 5L,
                    careerYears = 3,
                    summary = "백엔드 개발자"
                )
            }
        }

        @Test
        fun `이메일 인증 안되었으면 예외 발생`() {
            // Given
            val request = createSignupCompleteRequest(email = email)
            every { emailVerificationService.isVerified(signupToken, email) } returns false

            // When & Then
            val exception = assertThrows(IllegalArgumentException::class.java) {
                facade.completeSignup(signupToken, request)
            }
            assertEquals("이메일 인증이 완료되지 않았습니다.", exception.message)

            verify(exactly = 0) { memberWriteService.signupWithOAuth(any(), any(), any(), any(), any(), any()) }
            verify(exactly = 0) { tokenService.generateAuthTokens(any(), any()) }
        }

        @Test
        fun `회원가입 성공 시 세션 정리`() {
            // Given
            val request = createSignupCompleteRequest(email = email)
            val oAuthUser = createOAuthUser()
            val member = createMember()
            val tokens = createAuthTokens()

            every { emailVerificationService.isVerified(signupToken, email) } returns true
            every { sessionService.getOAuthUser(signupToken) } returns oAuthUser
            every { memberWriteService.signupWithOAuth(any(), any(), any(), any(), any(), any()) } returns member
            every { tokenService.generateAuthTokens(any(), any()) } returns tokens
            every { sessionService.clear(signupToken) } just Runs
            every { emailVerificationService.clearVerified(signupToken) } just Runs

            // When
            facade.completeSignup(signupToken, request)

            // Then - 세션 정리 반드시 호출
            verify(exactly = 1) { sessionService.clear(signupToken) }
            verify(exactly = 1) { emailVerificationService.clearVerified(signupToken) }
        }

        @Test
        fun `필수 필드만으로 회원가입 가능`() {
            // Given
            val request = createSignupCompleteRequest(
                email = email,
                username = "minimumuser",
                currentPositionId = null,
                careerYears = null,
                summary = null
            )
            val oAuthUser = createOAuthUser()
            val member = createMember()
            val tokens = createAuthTokens()

            every { emailVerificationService.isVerified(signupToken, email) } returns true
            every { sessionService.getOAuthUser(signupToken) } returns oAuthUser
            every {
                memberWriteService.signupWithOAuth(
                    email = email,
                    username = "minimumuser",
                    oAuthUser = oAuthUser,
                    currentPositionId = null,
                    careerYears = null,
                    summary = null
                )
            } returns member
            every { tokenService.generateAuthTokens(any(), any()) } returns tokens
            every { sessionService.clear(signupToken) } just Runs
            every { emailVerificationService.clearVerified(signupToken) } just Runs

            // When
            val result = facade.completeSignup(signupToken, request)

            // Then
            assertNotNull(result)
            assertEquals(100L, result.memberId)
        }
    }
}