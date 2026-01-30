package com.hirelog.api.auth.infra.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date

/**
 * JWT 유틸리티 (Access Token 전용)
 *
 * 책임:
 * - Access Token 발급
 * - Access Token 검증
 * - 인증/인가에 필요한 최소 정보 추출
 *
 * 제외:
 * - Refresh Token ❌
 * - Signup Token ❌
 * - Cookie 처리 ❌
 */
@Component
class JwtUtils(
    private val jwtProperties: JwtProperties,
) {

    /**
     * 서명 키
     *
     * - HMAC-SHA256
     * - secret 길이는 최소 256bit 이상 권장
     */
    private val signingKey = Keys.hmacShaKeyFor(
        jwtProperties.secret.toByteArray()
    )

    /**
     * Access Token 발급
     *
     * @param memberId 인증 주체
     * @param role 인가 판단용 역할
     */
    fun issueAccessToken(
        memberId: Long,
        role: String,
    ): String {
        val now = Date()
        val expiry = Date(now.time + jwtProperties.accessExpirationMs)

        // role 앞에 ROLE_ 접두어가 없다면 붙여줍니다.
        val authority = if (role.startsWith("ROLE_")) role else "ROLE_$role"

        return Jwts.builder()
            .subject(memberId.toString())   // 인증 주체
            .claim("role", role)            // 인가 정보
            .issuedAt(now)                  // 발급 시간
            .expiration(expiry)             // 만료 시간
            .signWith(signingKey)           // 서명 (HS256)
            .compact()
    }

    /**
     * 토큰 유효성 검증
     *
     * 검증 항목:
     * - 서명
     * - 만료 시간
     */
    fun validate(token: String): Boolean =
        runCatching {
            parseClaims(token)
        }.isSuccess

    /**
     * memberId 추출
     */
    fun getMemberId(token: String): Long =
        parseClaims(token).subject.toLong()

    /**
     * role 추출
     */
    fun getRole(token: String): String =
        parseClaims(token)["role"] as String

    /**
     * Claims 파싱
     *
     * 주의:
     * - 내부 전용
     * - 예외는 상위로 전달
     */
    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload


    fun parseClaimsSafely(token: String): Claims? =
        runCatching {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        }.getOrNull()
}
