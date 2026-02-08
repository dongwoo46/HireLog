package com.hirelog.api.common.application.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.common.domain.kafka.FailedEventStatus
import com.hirelog.api.common.domain.kafka.FailedKafkaEvent
import com.hirelog.api.common.infra.persistence.jpa.repository.FailedKafkaEventJpaRepository
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.payload.JobSummaryOutboxPayload
import com.hirelog.api.job.application.summary.payload.JobSummarySearchPayload
import com.hirelog.api.job.infra.kafka.topic.JdKafkaTopics
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Failed Kafka Event 재처리 스케줄러
 *
 * 책임:
 * - DLT로 전송된 실패 이벤트 재처리
 * - 재처리 성공 시 REPROCESSED 상태로 전환
 * - 반복 실패 시 IGNORED 상태로 전환 (수동 확인 필요)
 *
 * 대상 토픽:
 * - outbox.event.job_summary (JobSummary OpenSearch 인덱싱)
 */
@Component
class FailedKafkaEventReprocessor(
    private val repository: FailedKafkaEventJpaRepository,
    private val openSearchAdapter: JobSummaryOpenSearchAdapter,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val BATCH_SIZE = 100
        private const val MAX_REPROCESS_AGE_HOURS = 72L
        private const val MAX_REPROCESS_ATTEMPTS = 3
    }

    /**
     * 매일 04:00에 실패 이벤트 재처리
     */
    @Scheduled(cron = "0 0 4 * * *")
    fun reprocessFailedEvents() {
        log.info("[FAILED_EVENT_REPROCESS_START]")

        val failedEvents = repository.findByStatus(
            status = FailedEventStatus.FAILED,
            pageable = PageRequest.of(0, BATCH_SIZE)
        )

        if (failedEvents.isEmpty) {
            log.info("[FAILED_EVENT_REPROCESS_SKIP] No failed events found")
            return
        }

        log.info("[FAILED_EVENT_REPROCESS_FOUND] count={}", failedEvents.totalElements)

        var successCount = 0
        var failCount = 0
        var ignoredCount = 0

        failedEvents.forEach { event ->
            // 72시간 이상 지난 이벤트는 IGNORED 처리
            if (event.failedAt.isBefore(LocalDateTime.now().minusHours(MAX_REPROCESS_AGE_HOURS))) {
                markAsIgnored(event, "Event is older than ${MAX_REPROCESS_AGE_HOURS} hours")
                ignoredCount++
                return@forEach
            }

            try {
                reprocessSingleEvent(event)
                successCount++
            } catch (e: Exception) {
                log.error(
                    "[FAILED_EVENT_REPROCESS_ERROR] id={}, topic={}, error={}",
                    event.id, event.topic, e.message, e
                )
                failCount++
            }
        }

        log.info(
            "[FAILED_EVENT_REPROCESS_COMPLETE] success={}, fail={}, ignored={}",
            successCount, failCount, ignoredCount
        )
    }

    private fun reprocessSingleEvent(event: FailedKafkaEvent) {
        when {
            event.topic.contains(JdKafkaTopics.OUTBOX_JOB_SUMMARY) ||
            event.topic == JdKafkaTopics.OUTBOX_JOB_SUMMARY -> {
                reprocessJobSummaryIndexing(event)
            }
            else -> {
                log.warn(
                    "[FAILED_EVENT_UNKNOWN_TOPIC] id={}, topic={}",
                    event.id, event.topic
                )
                markAsIgnored(event, "Unknown topic: ${event.topic}")
            }
        }
    }

    @Transactional
    fun reprocessJobSummaryIndexing(event: FailedKafkaEvent) {
        val payload = event.recordValue
            ?: throw IllegalStateException("recordValue is null for event ${event.id}")

        log.info(
            "[FAILED_EVENT_REPROCESS_ATTEMPT] id={}, topic={}, aggregateId={}",
            event.id, event.topic, event.recordKey
        )

        // Debezium + JsonConverter 조합에서 이중 직렬화 처리
        val actualPayload = if (payload.startsWith("\"")) {
            objectMapper.readValue<String>(payload)
        } else {
            payload
        }

        val outboxPayload = objectMapper.readValue<JobSummaryOutboxPayload>(actualPayload)
        val searchPayload = JobSummarySearchPayload.from(outboxPayload)

        openSearchAdapter.index(searchPayload)

        event.markReprocessed()
        repository.save(event)

        log.info(
            "[FAILED_EVENT_REPROCESS_SUCCESS] id={}, aggregateId={}",
            event.id, event.recordKey
        )
    }

    @Transactional
    fun markAsIgnored(event: FailedKafkaEvent, reason: String) {
        event.markIgnored()
        repository.save(event)

        log.warn(
            "[FAILED_EVENT_MARKED_IGNORED] id={}, topic={}, reason={}",
            event.id, event.topic, reason
        )
    }

    /**
     * 수동 재처리 (Admin API용)
     */
    @Transactional
    fun reprocessById(eventId: Long) {
        val event = repository.findById(eventId)
            .orElseThrow { IllegalArgumentException("FailedKafkaEvent not found: $eventId") }

        if (event.status != FailedEventStatus.FAILED) {
            throw IllegalStateException("Event is not in FAILED status: ${event.status}")
        }

        reprocessSingleEvent(event)
    }

    /**
     * 수동 무시 처리 (Admin API용)
     */
    @Transactional
    fun ignoreById(eventId: Long, reason: String) {
        val event = repository.findById(eventId)
            .orElseThrow { IllegalArgumentException("FailedKafkaEvent not found: $eventId") }

        markAsIgnored(event, reason)
    }
}
