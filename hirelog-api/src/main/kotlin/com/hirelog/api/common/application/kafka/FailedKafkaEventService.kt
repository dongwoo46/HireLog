package com.hirelog.api.common.application.kafka

import com.hirelog.api.common.domain.kafka.FailedKafkaEvent
import com.hirelog.api.common.infra.persistence.jpa.repository.FailedKafkaEventJpaRepository
import com.hirelog.api.common.logging.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * FailedKafkaEvent Application Service
 *
 * 책임:
 * - Kafka Consumer 실패 이벤트 저장
 * - 별도 트랜잭션으로 실행 (원본 트랜잭션과 분리)
 */
@Service
class FailedKafkaEventService(
    private val repository: FailedKafkaEventJpaRepository
) {

    /**
     * 실패 이벤트 저장
     *
     * 정책:
     * - 새 트랜잭션에서 실행 (REQUIRES_NEW)
     * - 저장 실패해도 DLT 전송은 진행되어야 함
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun save(
        topic: String,
        partition: Int,
        offset: Long,
        key: String?,
        value: String?,
        consumerGroup: String,
        exception: Exception,
        retryCount: Int
    ) {
        try {
            val failedEvent = FailedKafkaEvent.create(
                topic = topic,
                partitionNumber = partition,
                offsetNumber = offset,
                recordKey = key,
                recordValue = value,
                consumerGroup = consumerGroup,
                exception = exception,
                retryCount = retryCount
            )

            repository.save(failedEvent)

            log.info(
                "[FAILED_KAFKA_EVENT_SAVED] topic={}, partition={}, offset={}, consumerGroup={}, exceptionClass={}",
                topic, partition, offset, consumerGroup, exception.javaClass.simpleName
            )
        } catch (e: Exception) {
            // 저장 실패해도 DLT 전송은 진행
            log.error(
                "[FAILED_KAFKA_EVENT_SAVE_ERROR] topic={}, partition={}, offset={}, error={}",
                topic, partition, offset, e.message, e
            )
        }
    }
}
