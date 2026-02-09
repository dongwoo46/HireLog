package com.hirelog.api.auth.application

import com.hirelog.api.auth.infra.jwt.JwtUtils
import com.hirelog.api.common.infra.redis.RedisService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("TokenRefreshService 테스트")
class TokenRefreshServiceTest {

    private val jwtUtils: JwtUtils = mockk()
    private val redisService: RedisService = mockk(relaxed = true)
    
    private val service = TokenRefreshService(jwtUtils, redisService)

    @Test
    @DisplayName("토큰 갱신 성공: 유효한 Refresh Token이면 새 토큰을 발급하고 Rotation되어야 한다")
    fun refresh_success() {
        // Arrange
        val oldRefreshToken = "old-refresh-token"
        val memberId = 123L
        val newAccessToken = "new.access.token"
        
        every { redisService.get("REFRESH:$oldRefreshToken", String::class.java) } returns memberId.toString()
        every { jwtUtils.issueAccessToken(memberId, "USER") } returns newAccessToken

        // Act
        val result = service.refresh(oldRefreshToken)

        // Assert
        assertNotNull(result)
        assertEquals(newAccessToken, result?.accessToken)
        assertNotEquals(oldRefreshToken, result?.refreshToken)
        assertEquals(memberId, result?.memberId)
        
        // Rotation 검증: 기존 토큰 삭제 및 신규 토큰 저장
        verify(exactly = 1) { redisService.delete("REFRESH:$oldRefreshToken") }
        verify(exactly = 1) { 
            redisService.set(
                key = match { it.startsWith("REFRESH:") && it != "REFRESH:$oldRefreshToken" },
                value = memberId.toString(),
                duration = any<Duration>()
            ) 
        }
    }

    @Test
    @DisplayName("토큰 갱신 실패: Redis에 토큰이 없거나 만료되었으면 null을 반환해야 한다")
    fun refresh_fail_invalid_token() {
        // Arrange
        val invalidToken = "invalid-token"
        every { redisService.get("REFRESH:$invalidToken", String::class.java) } returns null

        // Act
        val result = service.refresh(invalidToken)

        // Assert
        assertNull(result)
        
        // 토큰 발급이나 Rotation 로직이 실행되지 않아야 함
        verify(exactly = 0) { jwtUtils.issueAccessToken(any(), any()) }
        verify(exactly = 0) { redisService.delete(any()) }
    }
}
