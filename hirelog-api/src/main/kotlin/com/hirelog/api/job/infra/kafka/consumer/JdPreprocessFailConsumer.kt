package com.hirelog.api.job.infra.kafka.consumer

import com.hirelog.api.common.application.processed.ProcessedEventService
import com.hirelog.api.common.domain.process.ProcessedEventId
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.messaging.JdPreprocessFailEvent
import com.hirelog.api.job.application.messaging.JdPreprocessFailHandler
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
 * - 멱등성 검사 후 Application Service에 위임
 *
 * 정책:
 * - DLT 전송 없음 (이미 실패 이벤트이므로)
 * - 도메인 상태 변경은 JdPreprocessFailHandler에서 트랜잭션으로 처리
 */
@Component
class JdPreprocessFailConsumer(
    private val processedEventService: ProcessedEventService,
    private val jdPreprocessFailHandler: JdPreprocessFailHandler
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

        // === 2. 도메인 상태 변경 (트랜잭션) ===
        jdPreprocessFailHandler.handle(event)

        // === 3. offset 커밋 ===
        acknowledgment.acknowledge()

        log.info(
            "[JD_PREPROCESS_FAIL_CONSUME_SUCCESS] eventId={}, requestId={}, errorCategory={}",
            event.eventId, event.requestId, event.errorCategory
        )
    }
}
