package com.hirelog.api.common.domain.kafka

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Kafka Consumer 실패 이벤트
 *
 * 책임:
 * - 최대 재시도 후에도 처리 실패한 Kafka 메시지 기록
 * - 추후 수동 재처리 / 모니터링 용도
 *
 * 정책:
 * - DLT 전송과 함께 DB에도 기록
 * - 재처리 시 status 업데이트
 */
@Entity
@Table(
    name = "failed_kafka_event",
    indexes = [
        Index(name = "idx_failed_kafka_event_topic", columnList = "topic"),
        Index(name = "idx_failed_kafka_event_status", columnList = "status"),
        Index(name = "idx_failed_kafka_event_created_at", columnList = "created_at")
    ]
)
class FailedKafkaEvent protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "topic", nullable = false, length = 255)
    val topic: String,

    @Column(name = "partition_number", nullable = false)
    val partitionNumber: Int,

    @Column(name = "offset_number", nullable = false)
    val offsetNumber: Long,

    @Column(name = "record_key", length = 500)
    val recordKey: String?,

    @Lob
    @Column(name = "record_value")
    val recordValue: String?,

    @Column(name = "consumer_group", nullable = false, length = 255)
    val consumerGroup: String,

    @Column(name = "exception_class", nullable = false, length = 500)
    val exceptionClass: String,

    @Lob
    @Column(name = "exception_message")
    val exceptionMessage: String?,

    @Lob
    @Column(name = "stack_trace")
    val stackTrace: String?,

    @Column(name = "retry_count", nullable = false)
    val retryCount: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: FailedEventStatus = FailedEventStatus.FAILED,

    @Column(name = "failed_at", nullable = false)
    val failedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "reprocessed_at")
    var reprocessedAt: LocalDateTime? = null

) : BaseEntity() {

    fun markReprocessed() {
        this.status = FailedEventStatus.REPROCESSED
        this.reprocessedAt = LocalDateTime.now()
    }

    fun markIgnored() {
        this.status = FailedEventStatus.IGNORED
    }

    companion object {
        fun create(
            topic: String,
            partitionNumber: Int,
            offsetNumber: Long,
            recordKey: String?,
            recordValue: String?,
            consumerGroup: String,
            exception: Exception,
            retryCount: Int
        ): FailedKafkaEvent {
            return FailedKafkaEvent(
                topic = topic,
                partitionNumber = partitionNumber,
                offsetNumber = offsetNumber,
                recordKey = recordKey,
                recordValue = recordValue?.take(10000), // 최대 10KB
                consumerGroup = consumerGroup,
                exceptionClass = exception.javaClass.name,
                exceptionMessage = exception.message?.take(2000),
                stackTrace = exception.stackTraceToString().take(5000),
                retryCount = retryCount
            )
        }
    }
}

