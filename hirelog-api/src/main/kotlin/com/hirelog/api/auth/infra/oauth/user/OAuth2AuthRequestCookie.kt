package com.hirelog.api.auth.infra.oauth.user

/**
 * OAuth Authorization Request 최소 보존 정보
 *
 * 책임:
 * - state 검증에 필요한 정보만 보관
 * - Java Serialization 완전 배제
 */
data class OAuth2AuthRequestCookie(
    val registrationId: String,
    val state: String,
    val redirectUri: String
)
