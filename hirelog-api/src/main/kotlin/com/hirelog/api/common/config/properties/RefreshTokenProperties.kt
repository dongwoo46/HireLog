package com.hirelog.api.common.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Refresh Token 설정 (Redis TTL)
 */
@ConfigurationProperties(prefix = "auth.refresh-token")
data class RefreshTokenProperties(
    val expirationMs: Long,
)
