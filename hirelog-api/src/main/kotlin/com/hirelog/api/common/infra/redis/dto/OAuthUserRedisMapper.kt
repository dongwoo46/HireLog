package com.hirelog.api.common.infra.redis.dto

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.auth.domain.OAuthUser

/**
 * OAuthUser ↔ Redis DTO 변환기
 *
 * 책임:
 * - enum ↔ string 변환
 * - Domain 복원은 오직 여기서만
 */
object OAuthUserRedisMapper {

    fun toDto(domain: OAuthUser): OAuthUserRedisDto =
        OAuthUserRedisDto(
            provider = domain.provider.name,
            providerUserId = domain.providerUserId,
            email = domain.email
        )

    fun toDomain(dto: OAuthUserRedisDto): OAuthUser =
        OAuthUser(
            provider = OAuth2Provider.valueOf(dto.provider),
            providerUserId = dto.providerUserId,
            email = dto.email
        )
}

