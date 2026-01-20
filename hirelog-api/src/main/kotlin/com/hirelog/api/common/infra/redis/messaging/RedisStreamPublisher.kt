package com.hirelog.api.common.infra.redis.messaging

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * Redis Stream Publisher
 *
 * 책임:
 * - Redis Stream에 메시지를 추가한다 (XADD)
 *
 * 비책임:
 * - 메시지 의미 해석 ❌
 * - Stream Key 정책 ❌
 * - 재시도 / DLQ ❌
 * - 도메인 객체 처리 ❌
 *
 * 설계 의도:
 * - Kafka Producer와 동일한 개념적 역할
 * - Map<String, String> 기반의 최소 메시지 계약
 */
@Component
class RedisStreamPublisher(
    private val redisTemplate: StringRedisTemplate
) {

    /**
     * Stream에 메시지 발행
     *
     * @param streamKey Redis Stream Key
     * @param message   Flat한 key-value 메시지
     */
    fun publish(
        streamKey: String,
        message: Map<String, String>
    ) {
        require(streamKey.isNotBlank()) { "streamKey must not be blank" }
        require(message.isNotEmpty()) { "message must not be empty" }

        // Redis Stream(XADD)에 메시지 추가
        redisTemplate
            .opsForStream<String, String>()
            .add(streamKey, message)
    }
}
