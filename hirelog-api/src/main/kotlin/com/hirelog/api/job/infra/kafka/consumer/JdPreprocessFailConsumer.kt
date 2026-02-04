package com.hirelog.api.job.infra.kafka.consumer

import com.hirelog.api.common.application.kafka.FailedKafkaEventService
import com.hirelog.api.common.application.processed.ProcessedEventService
import com.hirelog.api.common.domain.process.ProcessedEventId
import com.hirelog.api.common.exception.PipelineFailedException
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.messaging.JdPreprocessFailEvent
import com.hirelog.api.job.infra.kafka.topic.JdKafkaTopics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

/**
 * JdPreprocessFailConsumer
 *
 * 책임:
 * - Python 파이프라인에서 전처리 실패 시 발행한 이벤트 소비
 * - FailedKafkaEvent DB에 기록 (모니터링/재처리 용도)
 *
 * 정책:
 * - DLT 전송 없음 (이미 실패 이벤트이므로)
 * - DB 저장만 수행
 */
@Component
class JdPreprocessFailConsumer(
    private val processedEventService: ProcessedEventService,
    private val failedKafkaEventService: FailedKafkaEventService
) {

    companion object {
        private const val CONSUMER_GROUP = "jd-preprocess-fail-consumer"
    }

    @KafkaListener(
        topics = [JdKafkaTopics.PREPROCESS_RESPONSE_FAIL],
        groupId = CONSUMER_GROUP,
        containerFactory = "jdPreprocessFailListenerContainerFactory"
    )
    fun consume(
        event: JdPreprocessFailEvent,
        acknowledgment: Acknowledgment,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        log.info(
            "[JD_PREPROCESS_FAIL_CONSUME_START] eventId={}, requestId={}, errorCode={}, pipelineStage={}",
            event.eventId, event.requestId, event.errorCode, event.pipelineStage
        )

        // === 1. 멱등성 검사 ===
        val processedEventId = ProcessedEventId.create(event.eventId)
        val alreadyProcessed = processedEventService.isAlreadyProcessedOrMark(
            eventId = processedEventId,
            consumerGroup = CONSUMER_GROUP
        )

        if (alreadyProcessed) {
            log.info("[JD_PREPROCESS_FAIL_ALREADY_PROCESSED] eventId={}", event.eventId)
            acknowledgment.acknowledge()
            return
        }

        // === 2. FailedKafkaEvent DB 저장 ===
        val exception = PipelineFailedException(
            errorCode = event.errorCode,
            errorMessage = event.errorMessage,
            errorCategory = event.errorCategory,
            pipelineStage = event.pipelineStage,
            workerHost = event.workerHost
        )

        failedKafkaEventService.save(
            topic = event.kafkaMetadata?.originalTopic ?: topic,
            partition = event.kafkaMetadata?.originalPartition ?: partition,
            offset = event.kafkaMetadata?.originalOffset ?: offset,
            key = event.requestId,
            value = buildEventSummary(event),
            consumerGroup = CONSUMER_GROUP,
            exception = exception,
            retryCount = 0  // Python에서 이미 실패 판정된 이벤트
        )

        // === 3. offset 커밋 ===
        acknowledgment.acknowledge()

        log.info(
            "[JD_PREPROCESS_FAIL_CONSUME_SUCCESS] eventId={}, requestId={}, errorCategory={}",
            event.eventId, event.requestId, event.errorCategory
        )
    }

    private fun buildEventSummary(event: JdPreprocessFailEvent): String {
        return """
            |requestId=${event.requestId}
            |source=${event.source}
            |errorCode=${event.errorCode}
            |errorCategory=${event.errorCategory}
            |pipelineStage=${event.pipelineStage}
            |workerHost=${event.workerHost}
            |processingDurationMs=${event.processingDurationMs}
        """.trimMargin()
    }
}
