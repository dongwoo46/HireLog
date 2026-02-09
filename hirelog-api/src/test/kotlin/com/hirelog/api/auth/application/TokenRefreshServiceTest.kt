package com.hirelog.api.auth.application

import com.hirelog.api.auth.application.dto.TokenRefreshResult
import com.hirelog.api.auth.infra.jwt.JwtUtils
import com.hirelog.api.common.infra.redis.RedisService
import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.domain.MemberRole
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("TokenRefreshService 테스트")
class TokenRefreshServiceTest {

    private lateinit var tokenRefreshService: TokenRefreshService
    private lateinit var jwtUtils: JwtUtils
    private lateinit var redisService: RedisService
    private lateinit var memberCommand: MemberCommand

    @BeforeEach
    fun setUp() {
        jwtUtils = mockk()
        redisService = mockk()
        memberCommand = mockk()
        tokenRefreshService = TokenRefreshService(jwtUtils, redisService, memberCommand)
    }

    @Nested
    @DisplayName("refresh 메서드는")
    inner class RefreshTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 새 Access Token과 Refresh Token을 발급한다")
        fun shouldRefreshTokensSuccessfully() {
            // given
            val refreshToken = "valid-refresh-token"
            val memberId = 1L
            val memberRole = MemberRole.USER
            val newAccessToken = "new-access-token"
            val redisKey = "REFRESH:$refreshToken"

            val member = mockk<Member> {
                every { role } returns memberRole
            }

            every { redisService.get(redisKey, String::class.java) } returns memberId.toString()
            every { memberCommand.findById(memberId) } returns member
            every { jwtUtils.issueAccessToken(memberId, memberRole.name) } returns newAccessToken
            every { redisService.delete(any()) } just Runs
            every { redisService.set(any(), any(), any<Duration>()) } just Runs

            // when
            val result = tokenRefreshService.refresh(refreshToken)

            // then
            assertThat(result).isNotNull
            assertThat(result?.accessToken).isEqualTo(newAccessToken)
            assertThat(result?.refreshToken).isNotBlank()
            assertThat(result?.memberId).isEqualTo(memberId)

            verify(exactly = 1) { redisService.get(redisKey, String::class.java) }
            verify(exactly = 1) { memberCommand.findById(memberId) }
            verify(exactly = 1) { jwtUtils.issueAccessToken(memberId, memberRole.name) }
            verify(exactly = 1) { redisService.delete("REFRESH:$refreshToken") }
            verify(exactly = 1) { redisService.set(match { it.startsWith("REFRESH:") }, memberId.toString(), Duration.ofDays(7)) }
        }

        @Test
        @DisplayName("Redis에 존재하지 않는 Refresh Token은 null을 반환한다")
        fun shouldReturnNullWhenRefreshTokenNotInRedis() {
            // given
            val refreshToken = "invalid-refresh-token"
            val redisKey = "REFRESH:$refreshToken"

            every { redisService.get(redisKey, String::class.java) } returns null

            // when
            val result = tokenRefreshService.refresh(refreshToken)

            // then
            assertThat(result).isNull()

            verify(exactly = 1) { redisService.get(redisKey, String::class.java) }
            verify(exactly = 0) { memberCommand.findById(any()) }
            verify(exactly = 0) { jwtUtils.issueAccessToken(any(), any()) }
        }

        @Test
        @DisplayName("유효하지 않은 memberId 형식이면 null을 반환한다")
        fun shouldReturnNullWhenMemberIdIsInvalid() {
            // given
            val refreshToken = "valid-refresh-token"
            val redisKey = "REFRESH:$refreshToken"

            every { redisService.get(redisKey, String::class.java) } returns "invalid-id"

            // when
            val result = tokenRefreshService.refresh(refreshToken)

            // then
            assertThat(result).isNull()

            verify(exactly = 1) { redisService.get(redisKey, String::class.java) }
            verify(exactly = 0) { memberCommand.findById(any()) }
        }

        @Test
        @DisplayName("memberId에 해당하는 회원이 없으면 null을 반환한다")
        fun shouldReturnNullWhenMemberNotFound() {
            // given
            val refreshToken = "valid-refresh-token"
            val memberId = 999L
            val redisKey = "REFRESH:$refreshToken"

            every { redisService.get(redisKey, String::class.java) } returns memberId.toString()
            every { memberCommand.findById(memberId) } returns null

            // when
            val result = tokenRefreshService.refresh(refreshToken)

            // then
            assertThat(result).isNull()

            verify(exactly = 1) { redisService.get(redisKey, String::class.java) }
            verify(exactly = 1) { memberCommand.findById(memberId) }
            verify(exactly = 0) { jwtUtils.issueAccessToken(any(), any()) }
        }

        @Test
        @DisplayName("Refresh Token Rotation을 통해 기존 토큰은 삭제되고 새 토큰이 발급된다")
        fun shouldRotateRefreshToken() {
            // given
            val oldRefreshToken = "old-refresh-token"
            val memberId = 1L
            val memberRole = MemberRole.USER
            val newAccessToken = "new-access-token"
            val oldRedisKey = "REFRESH:$oldRefreshToken"

            val member = mockk<Member> {
                every { role } returns memberRole
            }

            every { redisService.get(oldRedisKey, String::class.java) } returns memberId.toString()
            every { memberCommand.findById(memberId) } returns member
            every { jwtUtils.issueAccessToken(memberId, memberRole.name) } returns newAccessToken
            every { redisService.delete(oldRedisKey) } just Runs
            every { redisService.set(any(), any(), any<Duration>()) } just Runs

            // when
            val result = tokenRefreshService.refresh(oldRefreshToken)

            // then
            assertThat(result).isNotNull
            assertThat(result?.refreshToken).isNotEqualTo(oldRefreshToken)

            verify(exactly = 1) { redisService.delete(oldRedisKey) }
            verify(exactly = 1) {
                redisService.set(
                    match { it.startsWith("REFRESH:") && it != oldRedisKey },
                    memberId.toString(),
                    Duration.ofDays(7)
                )
            }
        }
    }

    @Nested
    @DisplayName("invalidate 메서드는")
    inner class InvalidateTest {

        @Test
        @DisplayName("Refresh Token을 Redis에서 삭제한다")
        fun shouldInvalidateRefreshToken() {
            // given
            val refreshToken = "refresh-token-to-invalidate"
            val redisKey = "REFRESH:$refreshToken"

            every { redisService.delete(redisKey) } just Runs

            // when
            tokenRefreshService.invalidate(refreshToken)

            // then
            verify(exactly = 1) { redisService.delete(redisKey) }
        }
    }
}