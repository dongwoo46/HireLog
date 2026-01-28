package com.hirelog.api.common.infra.jpa.adapter

import com.hirelog.api.common.application.processed.ProcessedEventCommand
import com.hirelog.api.common.application.processed.ProcessedEventQuery
import com.hirelog.api.common.domain.ProcessedEvent
import com.hirelog.api.common.domain.ProcessedEventId
import com.hirelog.api.common.infra.jpa.ProcessedEventJpaEntity
import com.hirelog.api.common.infra.jpa.ProcessedEventJpaId
import com.hirelog.api.common.infra.jpa.repository.ProcessedEventJpaRepository
import org.springframework.stereotype.Component

/**
 * ProcessedEventPersistenceAdapter
 *
 * 책임:
 * - ProcessedEvent Port의 JPA 기반 구현
 * - 도메인 ↔ JPA 엔티티 변환
 */
@Component
class ProcessedEventJpaAdapter(
    private val repository: ProcessedEventJpaRepository
) : ProcessedEventQuery, ProcessedEventCommand {

    override fun exists(
        eventId: ProcessedEventId,
        consumerGroup: String
    ): Boolean {
        val id = ProcessedEventJpaId(
            eventId = eventId.value,
            consumerGroup = consumerGroup
        )
        return repository.existsById(id)
    }

    override fun save(processedEvent: ProcessedEvent) {
        val entity = ProcessedEventJpaEntity.create(
            eventId = processedEvent.eventId.value,
            consumerGroup = processedEvent.consumerGroup,
            processedAt = processedEvent.processedAt
        )
        repository.save(entity)
    }
}
