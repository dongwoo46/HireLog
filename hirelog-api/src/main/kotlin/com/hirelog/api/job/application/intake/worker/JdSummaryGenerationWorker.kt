package com.hirelog.api.job.application.intake.worker

import com.hirelog.api.common.async.MdcExecutor
import com.hirelog.api.common.infra.redis.messaging.RedisStreamConsumer
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.messaging.mapper.JdPreprocessResponseMessageMapper
import com.hirelog.api.job.application.summary.RedisSummaryGenerationFacade
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.infra.redis.JdStreamKeys
import jakarta.annotation.PreDestroy
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JdSummaryGenerationWorker
 *
 * 책임:
 * - JD 전처리 결과 Stream 소비 (TEXT/OCR/URL 공통)
 * - 메시지 → Application Command 변환
 * - 비동기 파이프라인 디스패치 + 동시성 제어
 *
 * 동시성 설계:
 * - Semaphore(MAX_LLM_CONCURRENCY): LLM 동시 호출 수 제한 + backpressure
 *   → Consumer 스레드가 semaphore.acquire()에서 blocking = 자연스러운 배압
 * - pipelineExecutor: Pre-LLM/Post-LLM 단계 실행용 스레드 풀
 * - MdcExecutor: pipelineExecutor를 래핑하여 MDC 전파 보장
 * - N개 Consumer: 병렬 메시지 처리 (CONSUMER_COUNT)
 *
 * Graceful Shutdown:
 * - 새 메시지 소비 중단 (running = false)
 * - in-flight 파이프라인 완료 대기 (awaitTermination)
 */
@Component
class JdSummaryGenerationWorker(
    private val redisConsumer: RedisStreamConsumer,
    private val messageMapper: JdPreprocessResponseMessageMapper,
    private val summaryGenerationFacade: RedisSummaryGenerationFacade
) {

    companion object {
        private const val MAX_LLM_CONCURRENCY = 4
        private const val PIPELINE_THREAD_POOL_SIZE = 4
        private const val SHUTDOWN_TIMEOUT_SECONDS = 60L
        private const val CONSUMER_GROUP = "jd-summary-generation-group"
        private const val CONSUMER_COUNT = 3
    }

    private val running = AtomicBoolean(true)
    private val semaphore = Semaphore(MAX_LLM_CONCURRENCY)
    private val pipelineExecutor: ExecutorService =
        Executors.newFixedThreadPool(PIPELINE_THREAD_POOL_SIZE)
    private val mdcPipelineExecutor = MdcExecutor(pipelineExecutor)

    fun startConsuming() {
        val streamKey = JdStreamKeys.PREPROCESS_RESPONSE
        val group = CONSUMER_GROUP
        val hostname = System.getenv("HOSTNAME") ?: "local"

        log.info(
            "[JD_SUMMARY_WORKER] startConsuming called, streamKey={}, group={}, consumerCount={}, maxConcurrency={}",
            streamKey,
            group,
            CONSUMER_COUNT,
            MAX_LLM_CONCURRENCY
        )

        repeat(CONSUMER_COUNT) { index ->
            val consumerName = "jd-summary-consumer-$hostname-$index"

            redisConsumer.sweepPendingMessages(
                streamKey = streamKey,
                group = group,
                consumer = consumerName
            ) { recordId, rawMessage ->
                dispatch(recordId, rawMessage)
            }

            redisConsumer.consume(
                streamKey = streamKey,
                group = group,
                consumer = consumerName
            ) { recordId, rawMessage ->
                dispatch(recordId, rawMessage)
            }

            log.info("[JD_SUMMARY_WORKER] Consumer started, consumer={}", consumerName)
        }
    }

    /**
     * 메시지 디스패치
     */
    private fun dispatch(
        recordId: String,
        rawMessage: Map<String, String>
    ): CompletableFuture<Void> {

        if (!running.get()) {
            return CompletableFuture.failedFuture(
                IllegalStateException("Worker is shutting down, rejecting message recordId=$recordId")
            )
        }

        semaphore.acquire()

        val preprocessMessage = messageMapper.from(rawMessage)
        MDC.put("requestId", preprocessMessage.requestId)

        // Message → Application Command 변환
        val command =
            JobSummaryGenerateCommand(
                requestId = preprocessMessage.requestId,
                brandName = preprocessMessage.brandName,
                positionName = preprocessMessage.positionName,
                source = preprocessMessage.source,
                sourceUrl = preprocessMessage.sourceUrl,
                canonicalMap = preprocessMessage.canonicalMap,
                recruitmentPeriodType = preprocessMessage.recruitmentPeriodType,
                openedDate = preprocessMessage.openedDate,
                closedDate = preprocessMessage.closedDate,
                skills = preprocessMessage.skills,
                occurredAt = System.currentTimeMillis()
            )

        log.info(
            "[JD_SUMMARY_DISPATCH] recordId={}, requestId={}, source={}",
            recordId,
            command.requestId,
            command.source
        )

        return summaryGenerationFacade
            .process(command, mdcPipelineExecutor)
            .whenComplete { _, ex ->
                semaphore.release()
                if (ex != null) {
                    log.error(
                        "[JD_SUMMARY_PIPELINE_FATAL] recordId={}, requestId={}",
                        recordId,
                        command.requestId,
                        ex
                    )
                }
                MDC.clear()
            }
    }

    @PreDestroy
    fun shutdown() {
        log.info("[JD_SUMMARY_WORKER_SHUTDOWN] Initiating graceful shutdown...")
        running.set(false)

        pipelineExecutor.shutdown()
        try {
            if (!pipelineExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                pipelineExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            pipelineExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }

        log.info("[JD_SUMMARY_WORKER_SHUTDOWN] Complete")
    }
}
