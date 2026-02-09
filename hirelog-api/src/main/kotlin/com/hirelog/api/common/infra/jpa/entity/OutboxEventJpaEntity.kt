package com.hirelog.api.common.infra.jpa.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID


/**
 * OutboxEventJpaEntity
 *
 * 역할:
 * - 트랜잭션 내에서 발생한 도메인 이벤트를 DB에 영속화하는 Outbox 엔티티
 * - Debezium CDC를 통해 Kafka 등 외부 메시징 시스템으로 전달되는 원본 데이터
 *
 * 설계 의도:
 * - "이벤트 발행" 자체를 DB 트랜잭션에 포함시키기 위함
 * - 애플리케이션이 Kafka publish 성공/실패를 직접 관리하지 않음
 * - WAL(Logical Replication) 기반 CDC를 전제로 함
 *
 * 핵심 원칙:
 * - 상태 컬럼(status, published_at 등) 없음
 * - 발행 여부는 DB가 아니라 메시징 인프라(Debezium + Kafka)가 책임
 * - Outbox 테이블은 "사실(fact) 기록" 용도로만 사용
 *
 * 주의:
 * - 이 엔티티는 비즈니스 로직을 절대 포함하지 않는다
 * - update/삭제를 전제로 사용하지 않는다 (append-only)
 */
@Entity
@Table(
    name = "outbox_event",
    indexes = [
        /**
         * aggregate_type 기준 라우팅 및 소비자 필터링 최적화용 인덱스
         * (Debezium Outbox EventRouter에서 topic routing에 사용 가능)
         */
        Index(name = "idx_outbox_aggregate_type", columnList = "aggregate_type"),

        /**
         * 시간 순서 기반 조회 및 운영 디버깅을 위한 인덱스
         * CDC 자체에는 필수는 아니나, 운영 관점에서 유용
         */
        Index(name = "idx_outbox_created_at", columnList = "created_at")
    ]
)
class OutboxEventJpaEntity(

    /**
     * Outbox 이벤트 식별자
     *
     * - 전역 유일 UUID
     * - Kafka 메시지 key로 사용 가능
     * - Debezium Outbox EventRouter의 event.id 필드와 매핑됨
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    /**
     * 이벤트를 발생시킨 Aggregate 종류
     *
     * 예:
     * - JOB_SUMMARY
     * - JOB_SNAPSHOT
     *
     * 용도:
     * - Kafka topic routing
     * - Consumer 측 이벤트 분기 처리
     */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    val aggregateType: String,

    /**
     * Aggregate 식별자
     *
     * 예:
     * - JobSummary.id
     * - JobSnapshot.id
     *
     * 용도:
     * - Kafka 메시지 key
     * - Consumer 측 멱등성 처리 기준
     */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    val aggregateId: String,

    /**
     * 이벤트 타입
     *
     * 예:
     * - JOB_SUMMARY_GENERATED
     * - JOB_SNAPSHOT_CREATED
     *
     * 의미:
     * - "무슨 일이 발생했는지"를 표현하는 명시적 이벤트 이름
     */
    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,

    /**
     * 이벤트 페이로드
     *
     * 특징:
     * - TEXT 타입 (Debezium Outbox EventRouter가 JSONB를 지원하지 않음)
     * - Consumer에서 이중 직렬화 방어 처리 필요
     * - 스키마 검증은 애플리케이션 레벨 책임
     */
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    val payload: String,

    /**
     * 이벤트 발생 시점
     *
     * 의미:
     * - 도메인 이벤트의 occurredAt과 동일
     * - DB에 기록된 순서와 WAL 순서가 일치함
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime
) {

    companion object {

        /**
         * 도메인 OutboxEvent → JPA 엔티티 변환
         *
         * 규칙:
         * - 반드시 트랜잭션 내부에서 호출
         * - 변환 외의 로직은 절대 포함하지 않음
         */
        fun fromDomain(
            event: com.hirelog.api.common.domain.outbox.OutboxEvent
        ): OutboxEventJpaEntity =
            OutboxEventJpaEntity(
                id = event.id,
                aggregateType = event.aggregateType,
                aggregateId = event.aggregateId,
                eventType = event.eventType,
                payload = event.payload,
                createdAt = event.occurredAt
            )
    }
}
