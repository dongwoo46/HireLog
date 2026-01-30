package com.hirelog.api.auth.infra.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT 설정 값 바인딩
 *
 * prefix: auth.jwt
 */
@ConfigurationProperties(prefix = "auth.jwt")
data class JwtProperties(

    /** JWT 서명용 시크릿 */
    val secret: String,

    /** Access Token 만료 시간 (ms) */
    val accessExpirationMs: Long,
)
