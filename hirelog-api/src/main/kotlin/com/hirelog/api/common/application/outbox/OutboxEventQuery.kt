package com.hirelog.api.common.application.outbox

import com.hirelog.api.common.domain.outbox.OutboxEvent
import com.hirelog.api.common.domain.outbox.OutboxStatus


/**
 * OutboxEventQuery
 *
 * 책임:
 * - 발행 대상 Outbox 이벤트 조회
 *
 * 주의:
 * - Publisher / Scheduler 전용
 * - 비즈니스 로직에서 사용 금지
 */
interface OutboxEventQuery {

    fun findByStatus(
        status: OutboxStatus,
        limit: Int
    ): List<OutboxEvent>
}
