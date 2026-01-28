package com.hirelog.api.auth.domain

/**
 * OAuth Provider가 인증한 사용자 식별 정보
 */
data class OAuthUser(
    val provider: OAuth2Provider,
    val providerUserId: String,
    val email: String?,
)
