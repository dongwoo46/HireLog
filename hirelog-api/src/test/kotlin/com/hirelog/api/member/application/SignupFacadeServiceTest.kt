package com.hirelog.api.member.application

import com.hirelog.api.auth.application.TokenService
import com.hirelog.api.auth.application.dto.AuthTokens
import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.member.application.dto.SignupResult
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.presentation.dto.BindRequest
import com.hirelog.api.member.presentation.dto.SignupCompleteRequest
import com.hirelog.api.member.presentation.dto.CheckEmailResponse
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("SignupFacadeService 테스트")
class SignupFacadeServiceTest {

    private lateinit var service: SignupFacadeService
    private lateinit var sessionService: SignupSessionService
    private lateinit var emailVerificationService: EmailVerificationService
    private lateinit var memberQuery: MemberQuery
    private lateinit var memberWriteService: MemberWriteService
    private lateinit var tokenService: TokenService
    private lateinit var recoverySessionService: RecoverySessionService

    private val validToken = "valid-token"
    private val validEmail = "user@example.com"
    private val authTokens = AuthTokens(accessToken = "access-jwt", refreshToken = "refresh-uuid")

    @BeforeEach
    fun setUp() {
        sessionService = mockk(relaxed = true)
        emailVerificationService = mockk(relaxed = true)
        memberQuery = mockk()
        memberWriteService = mockk()
        tokenService = mockk()
        recoverySessionService = mockk(relaxed = true)

        service = SignupFacadeService(
            sessionService,
            emailVerificationService,
            memberQuery,
            memberWriteService,
            tokenService,
            recoverySessionService
        )
    }

    @Nested
    @DisplayName("checkEmailAvailability 메서드는")
    inner class CheckEmailAvailabilityTest {

        @Test
        @DisplayName("이메일이 없으면 인증코드를 발송하고 exists=false를 반환한다")
        fun shouldSendCodeWhenEmailNotExists() {
            every { sessionService.validate(validToken) } just runs
            every { memberQuery.existsByEmail(validEmail) } returns false

            val result = service.checkEmailAvailability(validToken, validEmail)

            assertThat(result.exists).isFalse()
            verify { emailVerificationService.generateAndSave(validToken, validEmail) }
        }

        @Test
        @DisplayName("이메일이 이미 존재하면 코드 발송 없이 exists=true를 반환한다")
        fun shouldReturnExistsTrueWithoutSendingCode() {
            every { sessionService.validate(validToken) } just runs
            every { memberQuery.existsByEmail(validEmail) } returns true

            val result = service.checkEmailAvailability(validToken, validEmail)

            assertThat(result.exists).isTrue()
            verify(exactly = 0) { emailVerificationService.generateAndSave(any(), any()) }
        }

        @Test
        @DisplayName("세션이 유효하지 않으면 예외를 던진다")
        fun shouldThrowWhenSessionInvalid() {
            every { sessionService.validate("invalid-token") } throws
                IllegalArgumentException("잘못된 접근입니다.")

            assertThatThrownBy {
                service.checkEmailAvailability("invalid-token", validEmail)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("sendVerificationCode 메서드는")
    inner class SendVerificationCodeTest {

        @Test
        @DisplayName("이메일이 존재하면 인증코드를 발송한다")
        fun shouldSendCodeWhenEmailExists() {
            every { sessionService.validate(validToken) } just runs
            every { memberQuery.existsByEmail(validEmail) } returns true

            service.sendVerificationCode(validToken, validEmail)

            verify { emailVerificationService.generateAndSave(validToken, validEmail) }
        }

        @Test
        @DisplayName("이메일이 존재하지 않으면 예외를 던진다")
        fun shouldThrowWhenEmailNotExists() {
            every { sessionService.validate(validToken) } just runs
            every { memberQuery.existsByEmail("unknown@example.com") } returns false

            assertThatThrownBy {
                service.sendVerificationCode(validToken, "unknown@example.com")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("존재하지 않는 이메일")
        }
    }

    @Nested
    @DisplayName("bindOAuthAccount 메서드는")
    inner class BindOAuthAccountTest {

        @Test
        @DisplayName("이메일 인증이 완료되지 않으면 예외를 던진다")
        fun shouldThrowWhenEmailNotVerified() {
            every { emailVerificationService.isVerified(validToken, validEmail) } returns false

            assertThatThrownBy {
                service.bindOAuthAccount(validToken, BindRequest(email = validEmail))
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이메일 인증")
        }

        @Test
        @DisplayName("정상 흐름이면 SignupResult를 반환하고 세션을 정리한다")
        fun shouldReturnSignupResultAndClearSession() {
            val oAuthUser = OAuthUser(OAuth2Provider.GOOGLE, "google-123", validEmail)
            val member = mockk<Member> { every { id } returns 1L }

            every { emailVerificationService.isVerified(validToken, validEmail) } returns true
            every { sessionService.getOAuthUser(validToken) } returns oAuthUser
            every { memberWriteService.bindOAuthAccount(validEmail, oAuthUser) } returns member
            every { tokenService.generateAuthTokens(1L) } returns authTokens

            val result = service.bindOAuthAccount(validToken, BindRequest(email = validEmail))

            assertThat(result.memberId).isEqualTo(1L)
            assertThat(result.accessToken).isEqualTo("access-jwt")
            assertThat(result.refreshToken).isEqualTo("refresh-uuid")
            verify { sessionService.clear(validToken) }
            verify { emailVerificationService.clearVerified(validToken) }
        }
    }

    @Nested
    @DisplayName("completeSignup 메서드는")
    inner class CompleteSignupTest {

        private val request = SignupCompleteRequest(
            email = "user@example.com",
            username = "newuser"
        )

        @Test
        @DisplayName("이메일 인증이 완료되지 않으면 예외를 던진다")
        fun shouldThrowWhenEmailNotVerified() {
            every { emailVerificationService.isVerified(validToken, validEmail) } returns false

            assertThatThrownBy {
                service.completeSignup(validToken, request)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이메일 인증")
        }

        @Test
        @DisplayName("정상 흐름이면 SignupResult를 반환하고 세션을 정리한다")
        fun shouldReturnSignupResultAndClearSession() {
            val oAuthUser = OAuthUser(OAuth2Provider.GOOGLE, "google-123", validEmail)
            val member = mockk<Member> { every { id } returns 2L }

            every { emailVerificationService.isVerified(validToken, validEmail) } returns true
            every { sessionService.getOAuthUser(validToken) } returns oAuthUser
            every { memberWriteService.signupWithOAuth(
                email = validEmail,
                username = "newuser",
                oAuthUser = oAuthUser,
                currentPositionId = null,
                careerYears = null,
                summary = null
            ) } returns member
            every { tokenService.generateAuthTokens(2L) } returns authTokens

            val result = service.completeSignup(validToken, request)

            assertThat(result.memberId).isEqualTo(2L)
            verify { sessionService.clear(validToken) }
            verify { emailVerificationService.clearVerified(validToken) }
        }
    }

    @Nested
    @DisplayName("completeRecovery 메서드는")
    inner class CompleteRecoveryTest {

        private val recoveryToken = "recovery-token"
        private val request = SignupCompleteRequest(
            email = "user@example.com",
            username = "recovered"
        )

        @Test
        @DisplayName("복구 세션이 유효하지 않으면 예외를 던진다")
        fun shouldThrowWhenRecoverySessionInvalid() {
            every { recoverySessionService.validate(recoveryToken) } throws
                IllegalArgumentException("유효하지 않거나 만료된 복구 세션입니다.")

            assertThatThrownBy {
                service.completeRecovery(recoveryToken, request)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("이메일 인증이 완료되지 않으면 예외를 던진다")
        fun shouldThrowWhenEmailNotVerified() {
            every { recoverySessionService.validate(recoveryToken) } just runs
            every { emailVerificationService.isVerified(recoveryToken, validEmail) } returns false

            assertThatThrownBy {
                service.completeRecovery(recoveryToken, request)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이메일 인증")
        }

        @Test
        @DisplayName("정상 흐름이면 SignupResult를 반환하고 세션을 정리한다")
        fun shouldReturnSignupResultAndClearSession() {
            val member = mockk<Member> { every { id } returns 3L }

            every { recoverySessionService.validate(recoveryToken) } just runs
            every { emailVerificationService.isVerified(recoveryToken, validEmail) } returns true
            every { recoverySessionService.getMemberId(recoveryToken) } returns 3L
            every { memberWriteService.recoveryAccount(
                memberId = 3L,
                email = validEmail,
                username = "recovered",
                currentPositionId = null,
                careerYears = null,
                summary = null
            ) } returns member
            every { tokenService.generateAuthTokens(3L) } returns authTokens

            val result = service.completeRecovery(recoveryToken, request)

            assertThat(result.memberId).isEqualTo(3L)
            verify { recoverySessionService.clear(recoveryToken) }
            verify { emailVerificationService.clearVerified(recoveryToken) }
        }
    }
}
