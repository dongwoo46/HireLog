package com.hirelog.api.common.infra.jpa.entity

import com.hirelog.api.common.domain.outbox.OutboxStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * OutboxEventJpaEntity
 *
 * 역할:
 * - Outbox 패턴을 위한 이벤트 영속화 엔티티
 * - DB 트랜잭션과 메시지 발행 간의 경계 완충 지점
 *
 * 특징:
 * - 비즈니스 의미 없음
 * - 이벤트 전파 상태만 관리
 * - CDC(Debezium) 기반 발행 전제
 */
@Entity
@Table(
    name = "outbox_event",
    indexes = [
        Index(
            name = "idx_outbox_status_created_at",
            columnList = "status, created_at"
        )
    ]
)
class OutboxEventJpaEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    /**
     * 이벤트를 발생시킨 Aggregate 종류
     * 예: JD_SUMMARY_PROCESSING, JOB_SNAPSHOT
     */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    val aggregateType: String,

    /**
     * Aggregate 식별자
     */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    val aggregateId: String,

    /**
     * 이벤트 타입
     */
    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,

    /**
     * 이벤트 페이로드 (JSON 문자열)
     */
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    val payload: String,

    /**
     * 이벤트 전파 상태
     * - PENDING: 아직 외부로 발행되지 않음
     * - PUBLISHED: 발행 완료
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OutboxStatus,

    /**
     * 이벤트 발생 시점
     * (도메인 이벤트 occurredAt 과 동일 의미)
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,

    /**
     * 외부 발행 완료 시점
     */
    @Column(name = "published_at")
    var publishedAt: LocalDateTime?
) {

    companion object {

        /**
         * 신규 Outbox 이벤트 생성
         *
         * 규칙:
         * - 반드시 트랜잭션 내부에서 호출
         * - status는 항상 PENDING
         * - createdAt은 이벤트 발생 시점
         */
        fun create(
            id: UUID,
            aggregateType: String,
            aggregateId: String,
            eventType: String,
            payload: String,
            occurredAt: LocalDateTime = LocalDateTime.now()
        ): OutboxEventJpaEntity {
            return OutboxEventJpaEntity(
                id = id,
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload,
                status = OutboxStatus.PENDING,
                createdAt = occurredAt,
                publishedAt = null
            )
        }
    }


    /**
     * 발행 완료 마킹
     *
     * - CDC 이후 운영 보조 목적
     * - 비즈니스 로직 아님
     */
    fun markPublished() {
        this.status = OutboxStatus.PUBLISHED
        this.publishedAt = LocalDateTime.now()
    }
}
