package com.hirelog.api.common.application.outbox

import com.hirelog.api.common.domain.outbox.OutboxEvent
import java.util.*

/**
 * OutboxEventCommand
 *
 * 책임:
 * - Outbox 이벤트 영속화
 *
 * 성격:
 * - 비즈니스 Command 아님
 * - 트랜잭션 정합성 보조용 Command
 */
interface OutboxEventCommand {

    fun save(event: OutboxEvent)

}

