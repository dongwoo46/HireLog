package com.hirelog.api.job.infra.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.payload.JobSummaryOutboxPayload
import com.hirelog.api.job.application.summary.payload.JobSummarySearchPayload
import com.hirelog.api.job.infra.kafka.topic.JdKafkaTopics
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

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

        log.info(
            "[JOB_SUMMARY_INDEXING_START] aggregateId={}, partition={}, offset={}",
            aggregateId,
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

        // OutboxPayload → SearchPayload 변환
        val outboxPayload = objectMapper.readValue<JobSummaryOutboxPayload>(actualPayload)
        val searchPayload = JobSummarySearchPayload.from(outboxPayload)

        openSearchAdapter.index(searchPayload)

        acknowledgment.acknowledge()

        log.info(
            "[JOB_SUMMARY_INDEXING_SUCCESS] id={}, offset committed",
            searchPayload.id
        )
        // 예외 발생 시 ErrorHandler가 처리 (3회 재시도 → DLT 전송 → DB 기록)
    }
}
