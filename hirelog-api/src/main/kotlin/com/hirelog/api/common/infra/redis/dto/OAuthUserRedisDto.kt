package com.hirelog.api.common.infra.redis.dto

/**
 * Redis OAuth User Snapshot
 *
 * 책임:
 * - OAuth 인증 중 임시 상태 저장
 * - 직렬화 안정성 보장
 */
data class OAuthUserRedisDto(
    val provider: String = "",         // 기본값 추가
    val providerUserId: String = "",   // 기본값 추가
    val email: String? = null,
)
