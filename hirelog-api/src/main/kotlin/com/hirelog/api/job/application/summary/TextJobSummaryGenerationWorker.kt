package com.hirelog.api.job.application.summary

import com.hirelog.api.common.infra.redis.messaging.RedisStreamConsumer
import com.hirelog.api.common.logging.log
import com.hirelog.api.jd.application.messaging.JdStreamKeys
import com.hirelog.api.job.application.messaging.toJobSummaryPreprocessResponseMessage
import com.hirelog.api.job.application.summary.facade.JobSummaryGenerationFacadeService
import org.springframework.stereotype.Component

/**
 * TextJobSummaryGenerationWorker
 *
 * 책임:
 * - JD 전처리 결과 Stream 소비
 * - 메시지 → 계약 DTO 변환
 * - JobSummary 유스케이스 트리거
 *
 * 비책임:
 * - 중복 판정 ❌
 * - 요약 정책 ❌
 * - 저장 정책 ❌
 */
@Component
class TextJobSummaryGenerationWorker(
    private val redisConsumer: RedisStreamConsumer,
    private val jobSummaryFacadeService: JobSummaryGenerationFacadeService
) {

    /**
     * Stream 소비 시작
     *
     * Application bootstrap 시 1회 호출
     */
    fun startConsuming() {
        redisConsumer.consume(
            streamKey = JdStreamKeys.PREPROCESS_RESPONSE,
            group = "jd-summary-generation-group",
            consumer = "jd-summary-consumer-${System.getenv("HOSTNAME") ?: "local"}"
        ) { rawMessage ->
            handle(rawMessage)
        }
    }

    /**
     * 단일 메시지 처리
     */
    private fun handle(message: Map<String, String>) {
        val preprocessMessage = message.toJobSummaryPreprocessResponseMessage()

        log.info(
            "[JD_SUMMARY_CONSUME] requestId={} source={} textLength={}",
            preprocessMessage.requestId,
            preprocessMessage.source,
            preprocessMessage.canonicalText.length
        )

        jobSummaryFacadeService.generateFromPreprocessResult(
            preprocessMessage
        )
    }
}
