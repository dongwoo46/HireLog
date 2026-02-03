package com.hirelog.api.auth.application

import com.hirelog.api.auth.application.dto.AuthTokens
import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.auth.infra.jwt.JwtUtils
import com.hirelog.api.common.infra.redis.RedisService
import com.hirelog.api.common.infra.redis.dto.OAuthUserRedisMapper
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
class TokenService(
    private val jwtUtils: JwtUtils,
    private val redisService: RedisService,
) {

    /**
     * Access/Refresh 토큰 생성 및 Redis 저장
     */
    fun generateAuthTokens(memberId: Long, role: String): AuthTokens {
        // Access Token 생성 (JWT)
        val accessToken = jwtUtils.issueAccessToken(
            memberId = memberId,
            role = role
        )

        // Refresh Token 생성 (UUID) 및 Redis 저장
        val refreshToken = UUID.randomUUID().toString()
        redisService.set(
            key = "REFRESH:$refreshToken",
            value = memberId,
            duration = Duration.ofDays(7)
        )

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    /**
     * Signup 토큰 생성 및 Redis 저장
     */
    fun generateSignupToken(oauthUser: OAuthUser): String {
        val signupToken = UUID.randomUUID().toString()
        val redisDto = OAuthUserRedisMapper.toDto(oauthUser)

        redisService.set(
            key = "SIGNUP:$signupToken",
            value = redisDto,
            duration = Duration.ofMinutes(10)
        )

        return signupToken
    }
}
