package com.hirelog.api.common.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * OAuth 신규 가입용 임시 토큰 설정
 */
@ConfigurationProperties(prefix = "auth.signup-token")
data class SignupTokenProperties(
    val expirationMs: Long,
)
