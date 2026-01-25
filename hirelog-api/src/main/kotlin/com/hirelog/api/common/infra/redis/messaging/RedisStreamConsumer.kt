package com.hirelog.api.common.infra.redis.messaging

import com.hirelog.api.common.logging.log
import jakarta.annotation.PreDestroy
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Redis Stream Consumer
 *
 * 책임:
 * - Redis Stream 메시지를 Consumer Group 기반으로 지속 소비
 * - handler에 (recordId, message)를 전달하고 CompletableFuture를 수신
 * - Future 정상 완료 시에만 ACK 수행
 *
 * 설계 포인트:
 * - Consumer는 읽기 전용 역할만 담당 (XREADGROUP + ACK 위임)
 * - ACK는 전체 파이프라인 완료 시점에만 실행
 * - handler가 blocking하면 자연스럽게 backpressure 형성
 *
 * 비책임:
 * - 비즈니스 로직 실행 ❌
 * - 재시도 정책 ❌
 * - 동시성 제어 ❌ (handler 책임)
 */
@Component
class RedisStreamConsumer(
    private val redisTemplate: StringRedisTemplate
) {

    companion object {
        private const val RETRY_BACKOFF_MS = 3_000L
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(true)

    /**
     * Stream 소비 시작
     *
     * handler 계약:
     * - 인자: (recordId, message body)
     * - 반환: CompletableFuture<Void>
     *   - 정상 완료 → ACK 수행 (메시지 처리 완결)
     *   - 예외 완료 → ACK 안 함 (XPENDING으로 재처리 대상 유지)
     * - handler 내부에서 blocking 허용 (backpressure 수단)
     */
    fun consume(
        streamKey: String,
        group: String,
        consumer: String,
        handler: (recordId: String, message: Map<String, String>) -> CompletableFuture<Void>
    ) {
        require(streamKey.isNotBlank()) { "streamKey must not be blank" }
        require(group.isNotBlank()) { "group must not be blank" }
        require(consumer.isNotBlank()) { "consumer must not be blank" }

        createGroupIfAbsent(streamKey, group)

        executor.submit {
            val options = StreamReadOptions.empty()
                .count(1)
                .block(Duration.ofSeconds(2))

            val offset = StreamOffset.create(
                streamKey,
                ReadOffset.lastConsumed()
            )

            while (running.get() && !Thread.currentThread().isInterrupted) {
                try {
                    val records = redisTemplate.opsForStream<String, String>()
                        .read(
                            Consumer.from(group, consumer),
                            options,
                            offset
                        )

                    if (records.isNullOrEmpty()) continue

                    for (record in records) {
                        try {
                            val future = handler(record.id.value, record.value)

                            future.whenComplete { _, ex ->
                                if (ex == null) {
                                    redisTemplate.opsForStream<String, String>()
                                        .acknowledge(streamKey, group, record.id)
                                } else {
                                    log.warn(
                                        "[REDIS_STREAM_NACK] stream={} group={} recordId={} reason={}",
                                        streamKey, group, record.id.value,
                                        ex.message
                                    )
                                }
                            }
                        } catch (ex: Exception) {
                            log.error(
                                "[REDIS_STREAM_DISPATCH_ERROR] stream={} group={} recordId={}",
                                streamKey, group, record.id.value, ex
                            )
                        }
                    }
                } catch (ex: Exception) {
                    log.error(
                        "[REDIS_STREAM_READ_ERROR] stream={} group={} consumer={}",
                        streamKey, group, consumer, ex
                    )
                    Thread.sleep(RETRY_BACKOFF_MS)
                }
            }
        }
    }

    private fun createGroupIfAbsent(streamKey: String, group: String) {
        try {
            redisTemplate.opsForStream<String, String>()
                .createGroup(streamKey, ReadOffset.from("0-0"), group)
        } catch (ex: Exception) {
            if (ex.message?.contains("BUSYGROUP") == true) {
                log.debug("[REDIS_STREAM_GROUP_EXISTS] stream={} group={}", streamKey, group)
            } else {
                log.warn(
                    "[REDIS_STREAM_GROUP_CREATE_FAILED] stream={} group={} reason={}",
                    streamKey, group, ex.message
                )
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        running.set(false)
        executor.shutdownNow()
        log.info("[REDIS_STREAM_CONSUMER_SHUTDOWN]")
    }
}
