package com.hirelog.api.job.application.rag

import com.hirelog.api.common.exception.BusinessErrorCode
import com.hirelog.api.common.exception.BusinessException
import com.hirelog.api.common.logging.log
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * RAG 일일 호출 제한
 *
 * - USER: 하루 최대 3회 (Redis 카운터, 자정 TTL)
 * - ADMIN: 제한 없음
 *
 * 설계:
 * - Redis INCR으로 atomic increment → race condition 없음
 * - count == 1L (첫 호출)일 때 자정까지 TTL 설정
 * - count > DAILY_LIMIT이면 BusinessException(429)
 */
@Component
class RagRateLimiter(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    companion object {
        private const val DAILY_LIMIT = 3L
        private const val KEY_PREFIX = "rag:rate:limit"
    }

    fun checkAndIncrement(memberId: Long, isAdmin: Boolean) {
        if (isAdmin) return

        val key = "$KEY_PREFIX:$memberId:${LocalDate.now()}"
        val count = redisTemplate.opsForValue().increment(key) ?: 1L

        if (count == 1L) {
            val now = LocalDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val ttl = Duration.ofSeconds(ChronoUnit.SECONDS.between(now, midnight))
            redisTemplate.expire(key, ttl)
        }

        if (count > DAILY_LIMIT) {
            log.warn("[RAG_RATE_LIMIT_EXCEEDED] memberId={}, count={}", memberId, count)
            throw BusinessException(BusinessErrorCode.RAG_RATE_LIMIT_EXCEEDED)
        }

        log.info("[RAG_RATE_LIMIT] memberId={}, count={}/{}", memberId, count, DAILY_LIMIT)
    }
}