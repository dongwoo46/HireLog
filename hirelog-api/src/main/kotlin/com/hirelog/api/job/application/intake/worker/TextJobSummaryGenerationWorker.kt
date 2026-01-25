package com.hirelog.api.job.application.intake.worker

import com.hirelog.api.common.async.MdcExecutor
import com.hirelog.api.common.infra.redis.messaging.RedisStreamConsumer
import com.hirelog.api.common.logging.log
import com.hirelog.api.jd.application.messaging.JdStreamKeys
import com.hirelog.api.job.application.messaging.mapper.JdPreprocessResponseMessageMapper
import com.hirelog.api.job.application.summary.JobSummaryGenerationFacadeService
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
 * TextJobSummaryGenerationWorker
 *
 * 책임:
 * - JD 전처리 결과 Stream 소비 트리거
 * - 메시지 → 계약 DTO 변환
 * - 비동기 파이프라인 디스패치 + 동시성 제어
 *
 * 동시성 설계:
 * - Semaphore(MAX_LLM_CONCURRENCY): LLM 동시 호출 수 제한 + backpressure
 *   → Consumer 스레드가 semaphore.acquire()에서 blocking = 자연스러운 배압
 * - pipelineExecutor: Pre-LLM/Post-LLM 단계 실행용 스레드 풀
 * - MdcExecutor: pipelineExecutor를 래핑하여 MDC 전파 보장
 *
 * Graceful Shutdown:
 * - 새 메시지 소비 중단 (running = false)
 * - in-flight 파이프라인 완료 대기 (awaitTermination)
 */
@Component
class TextJobSummaryGenerationWorker(
    private val redisConsumer: RedisStreamConsumer,
    private val messageMapper: JdPreprocessResponseMessageMapper,
    private val jobSummaryFacadeService: JobSummaryGenerationFacadeService
) {

    companion object {
        private const val MAX_LLM_CONCURRENCY = 4
        private const val PIPELINE_THREAD_POOL_SIZE = 4
        private const val SHUTDOWN_TIMEOUT_SECONDS = 60L
    }

    private val running = AtomicBoolean(true)
    private val semaphore = Semaphore(MAX_LLM_CONCURRENCY)
    private val pipelineExecutor: ExecutorService =
        Executors.newFixedThreadPool(PIPELINE_THREAD_POOL_SIZE)
    private val mdcPipelineExecutor = MdcExecutor(pipelineExecutor)

    fun startConsuming() {
        log.info(
            "[JD_SUMMARY_WORKER] startConsuming called, streamKey={}, group={}, maxConcurrency={}",
            JdStreamKeys.PREPROCESS_RESPONSE,
            "jd-summary-generation-group",
            MAX_LLM_CONCURRENCY
        )

        redisConsumer.consume(
            streamKey = JdStreamKeys.PREPROCESS_RESPONSE,
            group = "jd-summary-generation-group",
            consumer = "jd-summary-consumer-${System.getenv("HOSTNAME") ?: "local"}"
        ) { recordId, rawMessage ->
            dispatch(recordId, rawMessage)
        }
    }

    /**
     * 메시지 디스패치
     *
     * 처리 흐름:
     * 1. Semaphore acquire (backpressure: Consumer 스레드 blocking)
     * 2. MDC에 requestId 설정 (MdcExecutor가 비동기 스레드로 전파)
     * 3. pipelineExecutor에서 Facade 비동기 파이프라인 시작
     * 4. 완료 시 semaphore release
     *
     * 반환된 Future의 의미:
     * - 정상 완료: 파이프라인 전체 처리 완결 (성공 또는 비즈니스 실패 모두 포함)
     * - 예외 완료: 인프라 장애 (DB 불가 등) → Consumer가 ACK하지 않음
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

        log.info(
            "[JD_SUMMARY_DISPATCH] recordId={}, requestId={}, source={}",
            recordId, preprocessMessage.requestId, preprocessMessage.source
        )

        return CompletableFuture.completedFuture(preprocessMessage)
            .thenComposeAsync(
                { message ->
                    jobSummaryFacadeService.generateAsync(message, mdcPipelineExecutor)
                },
                mdcPipelineExecutor
            )
            .whenComplete { _, ex ->
                semaphore.release()
                if (ex != null) {
                    log.error(
                        "[JD_SUMMARY_PIPELINE_FATAL] recordId={}, requestId={}",
                        recordId, preprocessMessage.requestId, ex
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
                log.warn(
                    "[JD_SUMMARY_WORKER_SHUTDOWN] Pipeline executor did not terminate within {}s, forcing shutdown",
                    SHUTDOWN_TIMEOUT_SECONDS
                )
                pipelineExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            pipelineExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }

        log.info("[JD_SUMMARY_WORKER_SHUTDOWN] Complete")
    }
}
