package com.hirelog.api.job.infra.kafka.consumer

import com.hirelog.api.common.application.processed.ProcessedEventService
import com.hirelog.api.common.domain.process.ProcessedEventId
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.messaging.JdPreprocessResponseEvent
import com.hirelog.api.job.application.messaging.JdPreprocessResponseEventMapper
import com.hirelog.api.job.application.summary.JobSummaryHandler
import com.hirelog.api.job.infra.kafka.topic.JdKafkaTopics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * JdPreprocessResponseConsumer
 *
 * 책임:
 * - JD 전처리 완료 이벤트 소비
 * - DB 기반 멱등성 검증
 * - 요약 생성 유스케이스 트리거
 *
 * Kafka 정책:
 * - manual ack
 * - 단건 처리
 * - 실패 시 ErrorHandler가 3회 재시도 후 DLT 전송
 */
@Component
class JdPreprocessResponseConsumer(
    private val processedEventService: ProcessedEventService,
    private val responseEventMapper: JdPreprocessResponseEventMapper,
    private val jobSummaryHandler: JobSummaryHandler
) {

    companion object {
        private const val CONSUMER_GROUP = "jd-preprocess-response-consumer"
        private const val PROCESS_TIMEOUT_SECONDS = 60L
    }

    @KafkaListener(
        topics = [JdKafkaTopics.PREPROCESS_RESPONSE],
        groupId = CONSUMER_GROUP,
        containerFactory = "jdPreprocessResponseListenerContainerFactory"
    )
    fun consume(
        event: JdPreprocessResponseEvent,
        acknowledgment: Acknowledgment
    ) {
        log.info("[JD_PREPROCESS_CONSUME_START] eventId={}", event.eventId)

        // === 1. Kafka 메시지 단위 멱등성 검사 ===
        val processedEventId = ProcessedEventId.create(event.eventId)

        val alreadyProcessed =
            processedEventService.isAlreadyProcessedOrMark(
                eventId = processedEventId,
                consumerGroup = CONSUMER_GROUP
            )

        if (alreadyProcessed) {
            log.info("[JD_PREPROCESS_ALREADY_PROCESSED] eventId={}", event.eventId)
            acknowledgment.acknowledge()
            return
        }

        // === 2. Event → Message(Command) 변환 ===
        val message = responseEventMapper.toSummaryCommand(event)

        // === 3. 파이프라인 실행 (동기 대기) ===
        // 예외 발생 시 ErrorHandler가 처리 (3회 재시도 → DLT 전송 → DB 기록)
        jobSummaryHandler
            .process(message)
            .get(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // === 4. 성공 → offset 커밋 ===
        acknowledgment.acknowledge()

        log.info("[JD_PREPROCESS_CONSUME_SUCCESS] eventId={}", event.eventId)
    }
}
