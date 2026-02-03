package com.hirelog.api.auth.infra.oauth.handler

import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.auth.infra.jwt.JwtUtils
import com.hirelog.api.common.infra.redis.dto.OAuthUserRedisMapper
import com.hirelog.api.common.infra.redis.RedisService
import com.hirelog.api.member.domain.Member
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class OAuth2TokenIssuer(
    private val jwtUtils: JwtUtils,
    private val redisService: RedisService, // 주입
) {

    /**
     * 기존 회원: Access Token(JWT) + Refresh Token(UUID/Redis)
     */
    fun issueAccessAndRefresh(member: Member, response: HttpServletResponse) {
        // 1. Access Token 생성 (JWT)
        val accessToken = jwtUtils.issueAccessToken(
            memberId = member.id,
            role = member.role.name // USER / ADMIN
        )
        // 2. Refresh Token 생성 (UUID) 및 Redis 저장
        val refreshToken = UUID.randomUUID().toString()
        redisService.set(
            key = "REFRESH:$refreshToken",
            value = member.id.toString(),
            duration = Duration.ofDays(7)
        )

        // 3. 쿠키 설정
        addCookie(response, "access_token", accessToken, 3600) // 1시간
        addCookie(response, "refresh_token", refreshToken, 604800) // 7일
    }

    /**
     * 신규 회원: Signup Token(UUID/Redis)
     */
    fun issueSignupToken(oauthUser: OAuthUser, response: HttpServletResponse) {
        // 1. 임시 가입 키 생성
        val signupKey = UUID.randomUUID().toString()

        // 2. Domain → Redis DTO 변환
        val redisDto = OAuthUserRedisMapper.toDto(oauthUser)

        // 3. Redis에 DTO 저장 (10분)
        redisService.set(
            key = "SIGNUP:$signupKey",
            value = redisDto,
            duration = Duration.ofMinutes(10)
        )

        // 3. 쿠키 설정
        addCookie(response, "signup_token", signupKey, 600) // 10분
    }

    private fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val cookie = Cookie(name, value).apply {
            path = "/"
            isHttpOnly = true
            // secure = true // HTTPS 환경에서 활성화
            this.maxAge = maxAge
        }
        response.addCookie(cookie)
    }
}