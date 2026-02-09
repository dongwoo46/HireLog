package com.hirelog.api.auth.application

import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.auth.infra.jwt.JwtUtils
import com.hirelog.api.common.infra.redis.RedisService
import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.application.port.MemberQuery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("TokenService 테스트")
class TokenServiceTest {

    private val jwtUtils: JwtUtils = mockk()
    private val redisService: RedisService = mockk(relaxed = true)
    private val memberCommand: MemberCommand = mockk()
    
    private val service = TokenService(jwtUtils, redisService, memberCommand)

    @Test
    @DisplayName("인증 토큰(Access/Refresh) 생성 성공")
    fun generateAuthTokens() {
        // Arrange
        val memberId = 1L
        val role = "USER"
        val expectedAccessToken = "access.token.jwt"
        
        every { jwtUtils.issueAccessToken(memberId, role) } returns expectedAccessToken

        // Act
        val tokens = service.generateAuthTokens(memberId)

        // Assert
        assertEquals(expectedAccessToken, tokens.accessToken)
        assertNotNull(tokens.refreshToken)
        
        // Refresh Token Redis 저장 검증
        verify(exactly = 1) { 
            redisService.set(
                key = match { it.startsWith("REFRESH:") },
                value = memberId,
                duration = Duration.ofDays(7)
            )
        }
    }

    @Test
    @DisplayName("회원가입 토큰 생성 성공")
    fun generateSignupToken() {
        // Arrange
        val oAuthUser = OAuthUser(
            email = "test@example.com",
            provider = OAuth2Provider.GOOGLE,
            providerUserId = "12345"
        )

        // Act
        val token = service.generateSignupToken(oAuthUser)

        // Assert
        assertNotNull(token)
        
        // Redis 저장 검증
        verify(exactly = 1) {
            redisService.set(
                key = match { it.startsWith("SIGNUP:") },
                value = any(),
                duration = Duration.ofMinutes(10)
            )
        }
    }
}
