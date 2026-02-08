package com.hirelog.api.member.application

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.common.infra.redis.RedisService
import com.hirelog.api.auth.infra.oauth.handler.dto.RecoverySessionRedisDto
import org.springframework.stereotype.Service
@Service
class RecoverySessionService(
    private val redisService: RedisService,
) {
    companion object {
        private const val RECOVERY_KEY_PREFIX = "RECOVERY:"
    }

    fun validate(token: String) {
        if (!redisService.hasKey("$RECOVERY_KEY_PREFIX$token")) {
            throw IllegalArgumentException("유효하지 않거나 만료된 복구 세션입니다.")
        }
    }

    fun getMemberId(token: String): Long =
        getDto(token).memberId

    fun getOAuthAccount(token: String): Pair<OAuth2Provider, String> {
        val dto = getDto(token)
        return dto.provider to dto.providerUserId
    }

    fun clear(token: String) {
        redisService.delete("$RECOVERY_KEY_PREFIX$token")
    }

    private fun getDto(token: String): RecoverySessionRedisDto =
        redisService.get(
            "$RECOVERY_KEY_PREFIX$token",
            RecoverySessionRedisDto::class.java
        ) ?: throw IllegalArgumentException("복구 세션이 만료되었습니다.")
}