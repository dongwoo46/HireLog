package com.hirelog.api.common.infra.redis.messaging

import com.hirelog.api.common.logging.log
import jakarta.annotation.PreDestroy
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Redis Stream Consumer
 *
 * 책임:
 * - Redis Stream 메시지를 Consumer Group 기반으로 지속 소비
 * - 메시지 처리 성공 시 ACK
 *
 * 설계 포인트:
 * - consume()는 "시작 트리거" 역할만 한다
 * - 실제 소비 루프는 백그라운드 스레드에서 동작한다
 *
 * 비책임:
 * - 비즈니스 로직 실행 ❌
 * - 재시도 정책 ❌
 * - DLQ 처리 ❌
 */
@Component
class RedisStreamConsumer(
    private val redisTemplate: StringRedisTemplate
) {

    /**
     * 단일 Consumer 전용 스레드
     *
     * - Redis Stream은 보통 blocking I/O 기반
     * - CPU 바운드 작업이 아니므로 단일 스레드로 충분
     */
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Consumer 실행 상태 플래그
     *
     * - graceful shutdown을 위해 AtomicBoolean 사용
     */
    private val running = AtomicBoolean(true)

    /**
     * Stream 소비 시작
     *
     * ⚠️ 주의:
     * - 이 메서드는 "한 번만" 호출되어야 한다
     * - 내부에서 무한 루프를 실행한다
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

        // 백그라운드 스레드에서 소비 루프 실행
        executor.submit {

            /**
             * XREADGROUP 옵션
             *
             * - COUNT 1: 한 번에 한 메시지 처리 (처리 안정성 우선)
             * - BLOCK 2s: 메시지 없으면 최대 2초 대기
             */
            val options = StreamReadOptions.empty()
                .count(1)
                .block(Duration.ofSeconds(2))

            /**
             * 마지막으로 ACK된 메시지 이후부터 읽음
             *
             * - Consumer Group의 offset 관리에 위임
             */
            val offset = StreamOffset.create(
                streamKey,
                ReadOffset.lastConsumed()
            )

            // Consumer 메인 루프
            while (running.get() && !Thread.currentThread().isInterrupted) {
                try {
                    val records = redisTemplate.opsForStream<String, String>()
                        .read(
                            Consumer.from(group, consumer),
                            options,
                            offset
                        )

                    if (records == null || records.isEmpty()) {
                        continue
                    }

                    for (record in records) {

                        try {
                            handler(record.value)

                            redisTemplate.opsForStream<String, String>()
                                .acknowledge(streamKey, group, record.id)

                        } catch (ex: Exception) {

                            // ✅ 비즈니스 처리 실패 (ACK ❌)
                            log.error(
                                "[REDIS_STREAM_HANDLER_ERROR] stream={} group={} consumer={} recordId={}",
                                streamKey,
                                group,
                                consumer,
                                record.id.value,
                                ex
                            )
                        }
                    }
                } catch (ex: Exception) {
                    /**
                     * Redis 연결 오류 / 일시적 장애
                     *
                     * - 루프를 깨지 않고 재시도
                     * - 필요 시 여기서 backoff 정책 추가 가능
                     */
                    // ✅ Redis read 자체 실패
                    log.error(
                        "[REDIS_STREAM_READ_ERROR] stream={} group={} consumer={}",
                        streamKey,
                        group,
                        consumer,
                        ex
                    )
                }
            }
        }
    }

    /**
     * Consumer Group 생성
     *
     * - 이미 존재하면 BUSYGROUP 예외 발생
     * - 멱등성을 위해 예외 무시
     */
    private fun createGroupIfAbsent(
        streamKey: String,
        group: String
    ) {
        try {
            redisTemplate.opsForStream<String, String>()
                .createGroup(streamKey, ReadOffset.from("0-0"), group)
        } catch (_: Exception) {
            // BUSYGROUP 등 무시
        }
    }

    /**
     * 애플리케이션 종료 시 호출
     *
     * - Consumer 루프 종료
     * - 스레드 정리
     */
    @PreDestroy
    fun shutdown() {
        running.set(false)
        executor.shutdownNow()
        log.info(
            "[REDIS_STREAM_CONSUMER_SHUTDOWN]"
        )
    }
}
