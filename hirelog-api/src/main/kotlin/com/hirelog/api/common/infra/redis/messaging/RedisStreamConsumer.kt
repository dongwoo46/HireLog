package com.hirelog.api.common.infra.redis.messaging

import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis Stream Consumer
 *
 * 책임:
 * - Redis Stream 메시지 수신 (XREADGROUP)
 * - 메시지 전달
 * - ACK 처리
 *
 * 비책임:
 * - 비즈니스 로직 실행 ❌
 * - 재시도 정책 ❌
 * - 메시지 의미 해석 ❌
 */
@Component
class RedisStreamConsumer(
    private val redisTemplate: StringRedisTemplate
) {

    /**
     * Stream 메시지 소비
     *
     * @param streamKey  Redis Stream Key 메시지 통로(topic)
     * @param group      Consumer Group  소비자그룹
     * @param consumer   Consumer Name  개별 워커
     * @param handler    메시지 처리 콜백 실제 처리 로직
     */
    fun consume(
        streamKey: String,
        group: String,
        consumer: String,
        handler: (Map<String, String>) -> Unit
    ) {
        require(streamKey.isNotBlank()) { "streamKey must not be blank" }
        require(group.isNotBlank()) { "group must not be blank" }
        require(consumer.isNotBlank()) { "consumer must not be blank" }

        // Consumer Group이 없으면 생성 (멱등)
        createGroupIfAbsent(streamKey, group)

        // XREADGROUP 설정
        val options = StreamReadOptions.empty()
            .count(1)
            .block(Duration.ofSeconds(2))

        val offset = StreamOffset.create(
            streamKey,
            ReadOffset.lastConsumed()
        )

        val records = redisTemplate.opsForStream<String, String>()
            .read(
                Consumer.from(group, consumer),
                options,
                offset
            ) ?: return

        for (record in records) {
            try {
                // 메시지 처리 (도메인 로직은 외부에서)
                handler(record.value)

                // 정상 처리된 경우에만 ACK
                redisTemplate.opsForStream<String, String>()
                    .acknowledge(streamKey, group, record.id)
            } catch (ex: Exception) {
                // 실패 시 ACK 하지 않음
                // 재처리 / DLQ 판단은 상위 레이어 책임
                throw ex
            }
        }
    }

    /**
     * Consumer Group 생성 (이미 존재하면 무시)
     */
    private fun createGroupIfAbsent(
        streamKey: String,
        group: String
    ) {
        try {
            redisTemplate.opsForStream<String, String>()
                .createGroup(streamKey, group)
        } catch (ex: Exception) {
            // BUSYGROUP 등 이미 존재하는 경우 무시
        }
    }
}
