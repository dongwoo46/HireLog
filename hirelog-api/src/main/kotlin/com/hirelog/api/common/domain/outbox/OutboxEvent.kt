package com.hirelog.api.common.domain.outbox

import java.time.LocalDateTime
import java.util.UUID

/**
 * OutboxEvent
 *
 * 의미:
 * - 트랜잭션 내에서 발생한
 *   "외부로 전파되어야 할 사건"
 *
 * 책임:
 * - 이벤트의 의미적 경계 표현
 *
 * 특징:
 * - 비즈니스 엔티티 아님
 * - 상태 전이 없음
 *
 * 주의:
 * - Kafka, CDC, JPA 등 기술 요소를 전혀 알지 않는다
 */
class OutboxEvent private constructor(
    val id: UUID,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    val occurredAt: LocalDateTime
) {

    init {
        require(aggregateType.isNotBlank())
        require(aggregateId.isNotBlank())
        require(eventType.isNotBlank())
    }

    companion object {

        /**
         * 신규 Outbox 이벤트 생성
         *
         * - 트랜잭션 안에서 생성됨
         * - "사건이 발생했다"는 사실만 표현
         */
        fun occurred(
            aggregateType: String,
            aggregateId: String,
            eventType: String,
            payload: String
        ): OutboxEvent {
            return OutboxEvent(
                id = UUID.randomUUID(),
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload,
                occurredAt = LocalDateTime.now()
            )
        }
    }
}
