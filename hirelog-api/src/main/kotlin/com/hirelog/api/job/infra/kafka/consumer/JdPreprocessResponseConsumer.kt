package com.hirelog.api.job.infra.kafka.consumer

import com.hirelog.api.common.application.processed.ProcessedEventService
import com.hirelog.api.common.domain.process.ProcessedEventId
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.messaging.JdPreprocessResponseEvent
import com.hirelog.api.job.application.messaging.JdPreprocessResponseEventMapper
import com.hirelog.api.job.application.summary.KafkaSummaryGenerationFacade
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

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
 * - 실패 시 offset 유지
 */
@Component
class JdPreprocessResponseConsumer(
    private val processedEventService: ProcessedEventService,
    private val responseEventMapper: JdPreprocessResponseEventMapper,
    private val summaryFacadeService: KafkaSummaryGenerationFacade
) {

    companion object {
        private const val CONSUMER_GROUP = "jd-preprocess-response-consumer"
    }

    @KafkaListener(
        topics = ["jd.preprocess.response"],
        groupId = CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(
        event: JdPreprocessResponseEvent,
        acknowledgment: Acknowledgment
    ) {
        // === 1️⃣ Kafka 메시지 단위 멱등성 검사 ===
        val processedEventId = ProcessedEventId.create(event.eventId)

        val alreadyProcessed =
            processedEventService.isAlreadyProcessedOrMark(
                eventId = processedEventId,
                consumerGroup = CONSUMER_GROUP
            )

        if (alreadyProcessed) {
            acknowledgment.acknowledge()
            return
        }

        // === 2️⃣ Event → Message(Command) 변환 ===
        val message = responseEventMapper.toSummaryCommand(event)

        // === 3️⃣ 비동기 파이프라인 실행 ===
        summaryFacadeService
            .process(message)
            .whenComplete { _, ex ->
                if (ex == null) {
                    // === 4️⃣ 성공 → offset 커밋 ===
                    acknowledgment.acknowledge()
                } else {
                    // === 5️⃣ 실패 → 커밋하지 않음 (Kafka 재전달) ===
                    // 로그만 남기고 예외는 삼킨다 (컨테이너 스레드 보호)
                    log.error(
                        "[JD_SUMMARY_KAFKA_FAILED] eventId={}, offset not committed",
                        event.eventId,
                        ex
                    )
                }
            }
    }
}
