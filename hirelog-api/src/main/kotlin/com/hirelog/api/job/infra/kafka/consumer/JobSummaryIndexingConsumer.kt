package com.hirelog.api.job.infra.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.JobSummaryOutboxConstants.EventType
import com.hirelog.api.job.application.summary.payload.JobSummaryOutboxPayload
import com.hirelog.api.job.application.summary.payload.JobSummarySearchPayload
import com.hirelog.api.job.infra.kafka.topic.JdKafkaTopics
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * JobSummary OpenSearch 인덱싱 Consumer
 *
 * 책임:
 * - Debezium CDC Outbox 메시지 소비
 * - OpenSearch에 JobSummary 문서 인덱싱
 *
 * 메시지 구조 (Debezium Outbox Event Router):
 * - Key: aggregate_id (JobSummary.id)
 * - Value: payload JSON (JobSummarySearchPayload)
 *
 * Kafka 정책:
 * - manual ack
 * - 실패 시 offset 유지 (재시도)
 */
@Component
class JobSummaryIndexingConsumer(
    private val openSearchAdapter: JobSummaryOpenSearchAdapter,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val CONSUMER_GROUP = "job-summary-indexing-consumer"
        private const val EVENT_TYPE_HEADER = "eventType"
    }

    @KafkaListener(
        topics = [JdKafkaTopics.OUTBOX_JOB_SUMMARY],
        groupId = CONSUMER_GROUP,
        containerFactory = "stringListenerContainerFactory"
    )
    fun consume(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment
    ) {
        val aggregateId = record.key()
        val payload = record.value()
        val eventType = extractEventType(record)

        log.info(
            "[JOB_SUMMARY_INDEXING_START] aggregateId={}, eventType={}, partition={}, offset={}",
            aggregateId,
            eventType,
            record.partition(),
            record.offset()
        )

        // Debezium + JsonConverter 조합에서 TEXT 컬럼은 이중 직렬화될 수 있음
        // 예: "{\"id\":1,...}" (문자열로 감싸진 JSON)
        val actualPayload = if (payload.startsWith("\"")) {
            objectMapper.readValue<String>(payload)
        } else {
            payload
        }

        when (eventType) {
            EventType.DELETED -> handleDelete(aggregateId, actualPayload)
            else -> handleIndex(actualPayload)
        }

        acknowledgment.acknowledge()

        log.info(
            "[JOB_SUMMARY_INDEXING_SUCCESS] aggregateId={}, eventType={}, offset committed",
            aggregateId,
            eventType
        )
        // 예외 발생 시 ErrorHandler가 처리 (3회 재시도 → DLT 전송 → DB 기록)
    }

    /**
     * Kafka 헤더에서 eventType 추출
     *
     * Debezium Outbox Event Router가 eventType 컬럼을 header로 전달
     * header가 없으면 CREATED로 간주 (하위 호환성)
     */
    private fun extractEventType(record: ConsumerRecord<String, String>): String {
        val header = record.headers().lastHeader(EVENT_TYPE_HEADER)
        return header?.value()?.toString(StandardCharsets.UTF_8) ?: EventType.CREATED
    }

    /**
     * CREATED 이벤트 처리 - OpenSearch 인덱싱
     */
    private fun handleIndex(payload: String) {
        val outboxPayload = objectMapper.readValue<JobSummaryOutboxPayload>(payload)
        val searchPayload = JobSummarySearchPayload.from(outboxPayload)
        openSearchAdapter.index(searchPayload)
    }

    /**
     * DELETED 이벤트 처리 - OpenSearch 문서 삭제
     *
     * payload 형식: {"id": 123}
     */
    private fun handleDelete(aggregateId: String, payload: String) {
        val id = aggregateId.toLongOrNull()
            ?: run {
                // aggregateId가 숫자가 아니면 payload에서 추출
                val deletePayload = objectMapper.readTree(payload)
                deletePayload.get("id")?.asLong()
            }
            ?: throw IllegalArgumentException("Cannot extract id from DELETED event. aggregateId=$aggregateId, payload=$payload")

        openSearchAdapter.delete(id)
    }
}
