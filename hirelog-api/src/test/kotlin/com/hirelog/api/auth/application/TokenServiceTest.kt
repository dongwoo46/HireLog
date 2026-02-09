package com.hirelog.api.auth.application

import com.hirelog.api.auth.application.dto.AuthTokens
import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.auth.infra.jwt.JwtUtils
import com.hirelog.api.auth.infra.oauth.handler.dto.OAuthUserRedisDto
import com.hirelog.api.auth.infra.oauth.handler.dto.OAuthUserRedisMapper
import com.hirelog.api.common.infra.redis.RedisService
import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.domain.MemberRole
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("TokenService 테스트")
class TokenServiceTest {

    private lateinit var tokenService: TokenService
    private lateinit var jwtUtils: JwtUtils
    private lateinit var redisService: RedisService
    private lateinit var memberCommand: MemberCommand

    @BeforeEach
    fun setUp() {
        jwtUtils = mockk()
        redisService = mockk()
        memberCommand = mockk()
        tokenService = TokenService(jwtUtils, redisService, memberCommand)
    }

    @Nested
    @DisplayName("generateAuthTokens 메서드는")
    inner class GenerateAuthTokensTest {

        @Test
        @DisplayName("유효한 memberId로 Access Token과 Refresh Token을 생성한다")
        fun shouldGenerateAuthTokensSuccessfully() {
            // given
            val memberId = 1L
            val memberRole = MemberRole.USER
            val accessToken = "generated-access-token"

            val member = mockk<Member> {
                every { role } returns memberRole
            }

            every { memberCommand.findById(memberId) } returns member
            every { jwtUtils.issueAccessToken(memberId, memberRole.name) } returns accessToken
            every { redisService.set(any<String>(), any<Long>(), any<Duration>()) } just Runs

            // when
            val result = tokenService.generateAuthTokens(memberId)

            // then
            assertThat(result).isNotNull
            assertThat(result.accessToken).isEqualTo(accessToken)
            assertThat(result.refreshToken).isNotBlank()

            verify(exactly = 1) { memberCommand.findById(memberId) }
            verify(exactly = 1) { jwtUtils.issueAccessToken(memberId, memberRole.name) }
            verify(exactly = 1) {
                redisService.set(
                    match { it.startsWith("REFRESH:") },
                    memberId,
                    Duration.ofDays(7)
                )
            }
        }

        @Test
        @DisplayName("USER 역할을 가진 회원의 토큰에 USER role이 포함된다")
        fun shouldIncludeUserRoleInToken() {
            // given
            val memberId = 1L
            val memberRole = MemberRole.USER
            val accessToken = "user-access-token"

            val member = mockk<Member> {
                every { role } returns memberRole
            }

            every { memberCommand.findById(memberId) } returns member
            every { jwtUtils.issueAccessToken(memberId, "USER") } returns accessToken
            every { redisService.set(any<String>(), any<Long>(), any<Duration>()) } just Runs

            // when
            val result = tokenService.generateAuthTokens(memberId)

            // then
            assertThat(result).isNotNull
            assertThat(result.accessToken).isEqualTo(accessToken)

            verify(exactly = 1) { jwtUtils.issueAccessToken(memberId, "USER") }
            verify(exactly = 0) { jwtUtils.issueAccessToken(memberId, "ADMIN") }
            verify(exactly = 0) { jwtUtils.issueAccessToken(memberId, "COMPANY") }
        }

        @Test
        @DisplayName("ADMIN 역할을 가진 회원의 토큰에 ADMIN role이 포함된다")
        fun shouldIncludeAdminRoleInToken() {
            // given
            val memberId = 2L
            val memberRole = MemberRole.ADMIN
            val accessToken = "admin-access-token"

            val member = mockk<Member> {
                every { role } returns memberRole
            }

            every { memberCommand.findById(memberId) } returns member
            every { jwtUtils.issueAccessToken(memberId, "ADMIN") } returns accessToken
            every { redisService.set(any<String>(), any<Long>(), any<Duration>()) } just Runs

            // when
            val result = tokenService.generateAuthTokens(memberId)

            // then
            assertThat(result).isNotNull
            assertThat(result.accessToken).isEqualTo(accessToken)

            verify(exactly = 1) { jwtUtils.issueAccessToken(memberId, "ADMIN") }
            verify(exactly = 0) { jwtUtils.issueAccessToken(memberId, "USER") }
            verify(exactly = 0) { jwtUtils.issueAccessToken(memberId, "COMPANY") }
        }

        @Test
        @DisplayName("존재하지 않는 memberId로 호출 시 IllegalStateException을 발생시킨다")
        fun shouldThrowExceptionWhenMemberNotFound() {
            // given
            val memberId = 999L

            every { memberCommand.findById(memberId) } returns null

            // when & then
            assertThatThrownBy { tokenService.generateAuthTokens(memberId) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Member not found: $memberId")

            verify(exactly = 1) { memberCommand.findById(memberId) }
            verify(exactly = 0) { jwtUtils.issueAccessToken(any(), any()) }
            verify(exactly = 0) { redisService.set(any<String>(), any(), any<Duration>()) }
        }

        @Test
        @DisplayName("Refresh Token은 UUID 형식으로 생성되고 Redis에 저장된다")
        fun shouldStoreRefreshTokenInRedis() {
            // given
            val memberId = 1L
            val memberRole = MemberRole.ADMIN
            val accessToken = "access-token"

            val member = mockk<Member> {
                every { role } returns memberRole
            }

            val capturedKey = slot<String>()
            val capturedValue = slot<Long>()
            val capturedDuration = slot<Duration>()

            every { memberCommand.findById(memberId) } returns member
            every { jwtUtils.issueAccessToken(memberId, memberRole.name) } returns accessToken
            every {
                redisService.set(
                    capture(capturedKey),
                    capture(capturedValue),
                    capture(capturedDuration)
                )
            } just Runs

            // when
            val result = tokenService.generateAuthTokens(memberId)

            // then
            assertThat(capturedKey.captured).startsWith("REFRESH:")
            assertThat(capturedKey.captured).hasSize(44) // "REFRESH:" (8) + UUID (36)
            assertThat(capturedValue.captured).isEqualTo(memberId)
            assertThat(capturedDuration.captured).isEqualTo(Duration.ofDays(7))
        }

    }

    @Nested
    @DisplayName("generateSignupToken 메서드는")
    inner class GenerateSignupTokenTest {

        @Test
        @DisplayName("OAuth 사용자 정보로 Signup Token을 생성하고 Redis에 저장한다")
        fun shouldGenerateSignupTokenSuccessfully() {
            // given
            val oauthUser = OAuthUser(
                provider = OAuth2Provider.GOOGLE,
                providerUserId = "google-123",
                email = "test@example.com"
            )

            val redisDto = mockk<OAuthUserRedisDto>()

            mockkObject(OAuthUserRedisMapper)
            every { OAuthUserRedisMapper.toDto(oauthUser) } returns redisDto
            every { redisService.set(any<String>(), any<OAuthUserRedisDto>(), any<Duration>()) } just Runs

            // when
            val signupToken = tokenService.generateSignupToken(oauthUser)

            // then
            assertThat(signupToken).isNotBlank()
            assertThat(signupToken).hasSize(36) // UUID 길이

            verify(exactly = 1) { OAuthUserRedisMapper.toDto(oauthUser) }
            verify(exactly = 1) {
                redisService.set(
                    match { it.startsWith("SIGNUP:") },
                    redisDto,
                    Duration.ofMinutes(10)
                )
            }

            unmockkObject(OAuthUserRedisMapper)
        }

        @Test
        @DisplayName("Signup Token은 SIGNUP: prefix와 함께 Redis에 저장된다")
        fun shouldStoreSignupTokenWithCorrectPrefix() {
            // given
            val oauthUser = OAuthUser(
                provider = OAuth2Provider.KAKAO,
                providerUserId = "kakao-456",
                email = "kakao@example.com"
            )
            val redisDto = mockk<OAuthUserRedisDto>()

            val capturedKey = slot<String>()
            val capturedValue = slot<OAuthUserRedisDto>()
            val capturedDuration = slot<Duration>()

            mockkObject(OAuthUserRedisMapper)
            every { OAuthUserRedisMapper.toDto(oauthUser) } returns redisDto
            every {
                redisService.set(
                    capture(capturedKey),
                    capture(capturedValue),
                    capture(capturedDuration)
                )
            } just Runs

            // when
            val signupToken = tokenService.generateSignupToken(oauthUser)

            // then
            assertThat(capturedKey.captured).startsWith("SIGNUP:")
            assertThat(capturedKey.captured).isEqualTo("SIGNUP:$signupToken")
            assertThat(capturedValue.captured).isEqualTo(redisDto)
            assertThat(capturedDuration.captured).isEqualTo(Duration.ofMinutes(10))

            unmockkObject(OAuthUserRedisMapper)
        }

        @Test
        @DisplayName("각 호출마다 고유한 Signup Token을 생성한다")
        fun shouldGenerateUniqueSignupTokens() {
            // given
            val oauthUser = OAuthUser(
                provider = OAuth2Provider.KAKAO,
                providerUserId = "naver-789",
                email = "naver@example.com"
            )
            val redisDto = mockk<OAuthUserRedisDto>()

            mockkObject(OAuthUserRedisMapper)
            every { OAuthUserRedisMapper.toDto(oauthUser) } returns redisDto
            every { redisService.set(any<String>(), any<OAuthUserRedisDto>(), any<Duration>()) } just Runs

            // when
            val token1 = tokenService.generateSignupToken(oauthUser)
            val token2 = tokenService.generateSignupToken(oauthUser)
            val token3 = tokenService.generateSignupToken(oauthUser)

            // then
            assertThat(setOf(token1, token2, token3)).hasSize(3) // 모두 다른 값

            unmockkObject(OAuthUserRedisMapper)
        }

        @Test
        @DisplayName("이메일이 null인 OAuth 사용자도 처리할 수 있다")
        fun shouldHandleOAuthUserWithNullEmail() {
            // given
            val oauthUser = OAuthUser(
                provider = OAuth2Provider.GOOGLE,
                providerUserId = "google-no-email",
                email = null
            )
            val redisDto = mockk<OAuthUserRedisDto>()

            mockkObject(OAuthUserRedisMapper)
            every { OAuthUserRedisMapper.toDto(oauthUser) } returns redisDto
            every { redisService.set(any<String>(), any<OAuthUserRedisDto>(), any<Duration>()) } just Runs

            // when
            val signupToken = tokenService.generateSignupToken(oauthUser)

            // then
            assertThat(signupToken).isNotBlank()
            verify(exactly = 1) { OAuthUserRedisMapper.toDto(oauthUser) }

            unmockkObject(OAuthUserRedisMapper)
        }
    }
}