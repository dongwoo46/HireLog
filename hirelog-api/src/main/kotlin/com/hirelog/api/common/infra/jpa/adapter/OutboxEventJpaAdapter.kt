package com.hirelog.api.common.infra.jpa.adapter

import com.hirelog.api.common.application.outbox.OutboxEventCommand
import com.hirelog.api.common.domain.outbox.OutboxEvent
import com.hirelog.api.common.domain.outbox.OutboxStatus
import com.hirelog.api.common.infra.jpa.entity.OutboxEventJpaEntity
import com.hirelog.api.common.infra.jpa.repository.OutboxEventJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * OutboxEventJpaAdapter
 *
 * 책임:
 * - OutboxEvent 저장
 * - 트랜잭션 내 이벤트 영속화
 */
@Component
class OutboxEventJpaAdapter(
    private val repository: OutboxEventJpaRepository
) : OutboxEventCommand {

    override fun save(event: OutboxEvent) {
        repository.save(
            OutboxEventJpaEntity.create(
                id = event.id,
                aggregateType = event.aggregateType,
                aggregateId = event.aggregateId,
                eventType = event.eventType,
                payload = event.payload,
                occurredAt = event.occurredAt
            )
        )
    }

    override fun markPublished(eventId: UUID) {
        val entity = repository.findById(eventId)
            .orElseThrow {
                IllegalStateException("OutboxEvent not found. id=$eventId")
            }

        // Dirty Checking으로 UPDATE 자동 발생
        entity.markPublished()
    }
}

