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
        private const val PENDING_SWEEP_MIN_IDLE_MS = 60_000L  // 1분 이상 idle인 메시지만 claim
        private const val PENDING_SWEEP_BATCH_SIZE = 100
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

    /**
     * Pending 메시지 복구 (앱 재시작 시 호출)
     *
     * 정책:
     * - minIdleTime 이상 처리되지 않은 pending 메시지를 claim하여 재처리
     * - 동기적으로 처리 완료까지 대기 (startConsuming 전에 호출)
     * - 실패한 메시지는 XPENDING에 남아 다음 sweep 대상이 됨
     *
     * @return 처리된 메시지 수
     */
    fun sweepPendingMessages(
        streamKey: String,
        group: String,
        consumer: String,
        handler: (recordId: String, message: Map<String, String>) -> CompletableFuture<Void>
    ): Int {
        createGroupIfAbsent(streamKey, group)

        val pendingSummary = redisTemplate.opsForStream<String, String>()
            .pending(streamKey, group)

        val totalPending = pendingSummary?.totalPendingMessages ?: 0L
        if (totalPending == 0L) {
            log.info("[PENDING_SWEEP] No pending messages. stream={} group={}", streamKey, group)
            return 0
        }

        log.info(
            "[PENDING_SWEEP_START] stream={} group={} totalPending={}",
            streamKey, group, totalPending
        )

        var processedCount = 0
        val minIdleTime = Duration.ofMillis(PENDING_SWEEP_MIN_IDLE_MS)

        // Pending 메시지 목록 조회
        val pendingMessages = redisTemplate.opsForStream<String, String>()
            .pending(
                streamKey,
                Consumer.from(group, consumer),
                org.springframework.data.domain.Range.unbounded(),
                PENDING_SWEEP_BATCH_SIZE.toLong()
            )

        if (pendingMessages.isNullOrEmpty()) {
            log.info("[PENDING_SWEEP] No claimable pending messages for this consumer. stream={} group={}", streamKey, group)
            return 0
        }

        for (pending in pendingMessages) {
            // idle time 체크
            if (pending.idleTimeMs < PENDING_SWEEP_MIN_IDLE_MS) {
                log.debug(
                    "[PENDING_SWEEP_SKIP] Message not idle enough. recordId={} idleTimeMs={}",
                    pending.idAsString, pending.idleTimeMs
                )
                continue
            }

            try {
                // XCLAIM으로 소유권 획득 + 메시지 내용 가져오기
                val claimedRecords = redisTemplate.opsForStream<String, String>()
                    .claim(
                        streamKey,
                        group,
                        consumer,
                        minIdleTime,
                        RecordId.of(pending.idAsString)
                    )

                if (claimedRecords.isNullOrEmpty()) {
                    log.warn("[PENDING_SWEEP_CLAIM_EMPTY] recordId={}", pending.idAsString)
                    continue
                }

                val record = claimedRecords[0]
                log.info(
                    "[PENDING_SWEEP_CLAIMED] recordId={} deliveryCount={}",
                    record.id.value, pending.totalDeliveryCount
                )

                // 핸들러로 처리 (동기 대기)
                val future = handler(record.id.value, record.value)
                future.whenComplete { _, ex ->
                    if (ex == null) {
                        redisTemplate.opsForStream<String, String>()
                            .acknowledge(streamKey, group, record.id)
                        log.info("[PENDING_SWEEP_ACK] recordId={}", record.id.value)
                    } else {
                        log.warn(
                            "[PENDING_SWEEP_FAILED] recordId={} reason={}",
                            record.id.value, ex.message
                        )
                    }
                }.join()  // 동기 대기

                processedCount++

            } catch (ex: Exception) {
                log.error(
                    "[PENDING_SWEEP_ERROR] recordId={} error={}",
                    pending.idAsString, ex.message, ex
                )
            }
        }

        log.info(
            "[PENDING_SWEEP_COMPLETE] stream={} group={} processed={}",
            streamKey, group, processedCount
        )

        return processedCount
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
