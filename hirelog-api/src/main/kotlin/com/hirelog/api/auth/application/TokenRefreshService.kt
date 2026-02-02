package com.hirelog.api.auth.application

import com.hirelog.api.auth.application.dto.TokenRefreshResult
import com.hirelog.api.auth.infra.jwt.JwtUtils
import com.hirelog.api.common.infra.redis.RedisService
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

/**
 * Token Refresh Service
 *
 * 책임:
 * - Refresh Token 검증
 * - Access Token 재발급
 * - Refresh Token Rotation (선택적)
 */
@Service
class TokenRefreshService(
    private val jwtUtils: JwtUtils,
    private val redisService: RedisService
) {

    companion object {
        private const val REFRESH_TOKEN_PREFIX = "REFRESH:"
        private val REFRESH_TOKEN_TTL = Duration.ofDays(7)
        private const val DEFAULT_ROLE = "USER"
    }

    /**
     * Refresh Token으로 Access Token 재발급
     *
     * @param refreshToken 클라이언트가 제공한 Refresh Token
     * @return TokenRefreshResult (성공 시 새 토큰들, 실패 시 null)
     */
    fun refresh(refreshToken: String): TokenRefreshResult? {
        // 1. Redis에서 Refresh Token 검증
        val memberId = validateRefreshToken(refreshToken)
            ?: return null

        // 2. 새 Access Token 발급
        val newAccessToken = jwtUtils.issueAccessToken(memberId, DEFAULT_ROLE)

        // 3. Refresh Token Rotation (보안 강화)
        val newRefreshToken = rotateRefreshToken(refreshToken, memberId)

        return TokenRefreshResult(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            memberId = memberId
        )
    }

    /**
     * Refresh Token 검증
     *
     * @return memberId (유효한 경우), null (무효한 경우)
     */
    private fun validateRefreshToken(refreshToken: String): Long? {
        val key = "$REFRESH_TOKEN_PREFIX$refreshToken"
        val memberIdStr = redisService.get(key, String::class.java)
            ?: return null

        return memberIdStr.toLongOrNull()
    }

    /**
     * Refresh Token Rotation
     *
     * 보안 강화:
     * - 기존 Refresh Token 무효화
     * - 새 Refresh Token 발급
     * - Replay Attack 방지
     */
    private fun rotateRefreshToken(oldRefreshToken: String, memberId: Long): String {
        // 기존 토큰 삭제
        redisService.delete("$REFRESH_TOKEN_PREFIX$oldRefreshToken")

        // 새 토큰 생성 및 저장
        val newRefreshToken = UUID.randomUUID().toString()
        redisService.set(
            key = "$REFRESH_TOKEN_PREFIX$newRefreshToken",
            value = memberId.toString(),
            duration = REFRESH_TOKEN_TTL
        )

        return newRefreshToken
    }

    /**
     * Refresh Token 무효화 (로그아웃 시 사용)
     */
    fun invalidate(refreshToken: String) {
        redisService.delete("$REFRESH_TOKEN_PREFIX$refreshToken")
    }
}


