package com.hirelog.api.job.infra.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.common.logging.log
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

        try {
            // Debezium Outbox Event Router는 payload를 그대로 전달
            val searchPayload = objectMapper.readValue<JobSummarySearchPayload>(payload)

            openSearchAdapter.index(searchPayload)

            acknowledgment.acknowledge()

            log.info(
                "[JOB_SUMMARY_INDEXING_SUCCESS] id={}, offset committed",
                searchPayload.id
            )
        } catch (e: Exception) {
            // 실패 시 offset 커밋하지 않음 → Kafka 재전달
            log.error(
                "[JOB_SUMMARY_INDEXING_FAILED] aggregateId={}, offset NOT committed",
                aggregateId,
                e
            )
        }
    }
}
