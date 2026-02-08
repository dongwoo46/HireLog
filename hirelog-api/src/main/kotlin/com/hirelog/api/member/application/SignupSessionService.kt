package com.hirelog.api.member.application

import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.common.infra.redis.RedisService
import com.hirelog.api.auth.infra.oauth.handler.dto.OAuthUserRedisDto
import com.hirelog.api.auth.infra.oauth.handler.dto.OAuthUserRedisMapper
import org.springframework.stereotype.Service

/**
 * SignupSessionService
 *
 * 책임:
 * - OAuth 가입 세션(Redis) 관리
 * - 세션 검증, 조회, 삭제
 */
@Service
class SignupSessionService(
    private val redisService: RedisService,
) {
    companion object {
        private const val SIGNUP_KEY_PREFIX = "SIGNUP:"
    }

    /**
     * 세션 유효성 검증
     */
    fun validate(token: String) {
        if (!redisService.hasKey("$SIGNUP_KEY_PREFIX$token")) {
            throw IllegalArgumentException("잘못된 접근입니다. 가입 토큰이 유효하지 않습니다.")
        }
    }

    /**
     * OAuth 사용자 정보 조회
     */
    fun getOAuthUser(token: String): OAuthUser {
        val dto = redisService.get(
            "$SIGNUP_KEY_PREFIX$token",
            OAuthUserRedisDto::class.java
        ) ?: throw IllegalArgumentException("유효하지 않거나 만료된 가입 세션입니다.")

        return OAuthUserRedisMapper.toDomain(dto)
    }

    /**
     * 세션 삭제
     */
    fun clear(token: String) {
        redisService.delete("$SIGNUP_KEY_PREFIX$token")
    }
}
