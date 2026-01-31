package com.hirelog.api.common.infra.jpa.adapter

import com.hirelog.api.common.application.outbox.OutboxEventCommand
import com.hirelog.api.common.domain.outbox.OutboxEvent
import com.hirelog.api.common.infra.jpa.entity.OutboxEventJpaEntity
import com.hirelog.api.common.infra.jpa.repository.OutboxEventJpaRepository
import org.springframework.stereotype.Component

/**
 * OutboxEventJpaAdapter
 *
 * 역할:
 * - 도메인 OutboxEvent를 JPA 엔티티로 변환하여 영속화
 *
 * 설계 원칙:
 * - append-only (INSERT ONLY)
 * - UPDATE / DELETE 절대 금지
 * - 발행 상태 관리 ❌ (Debezium CDC 책임)
 */
@Component
class OutboxEventJpaAdapter(
    private val repository: OutboxEventJpaRepository
) : OutboxEventCommand {

    /**
     * Outbox 이벤트 저장
     *
     * 규칙:
     * - 반드시 Application Service 트랜잭션 내부에서 호출
     * - save 이후 즉시 커밋되면 CDC 대상이 됨
     */
    override fun save(event: OutboxEvent) {
        repository.save(
            OutboxEventJpaEntity.fromDomain(event)
        )
    }
}
