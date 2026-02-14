package com.hirelog.api.common.application.outbox

import com.hirelog.api.common.domain.outbox.OutboxEvent
import com.hirelog.api.common.logging.log
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.ConcurrentHashMap

/**
 * OutboxEventWriteService
 *
 * 책임:
 * - Outbox 이벤트 생성 및 상태 전이
 * - 트랜잭션 경계의 유일한 소유자
 *
 * 원칙:
 * - Adapter에는 트랜잭션을 두지 않는다
 * - 도메인 상태 변경과 Outbox 저장은 반드시 같은 트랜잭션
 */
@Service
class OutboxEventWriteService(
    private val outboxEventCommand: OutboxEventCommand,
    private val meterRegistry: MeterRegistry
) {

    private val counterCache = ConcurrentHashMap<String, Counter>()

    /**
     * Outbox 이벤트 생성
     *
     * 사용 위치:
     * - 도메인 상태 변경 직후
     * - 같은 트랜잭션 안에서 호출되어야 함
     */
    @Transactional
    fun append(event: OutboxEvent) {
        try {
            outboxEventCommand.save(event)
            if (TransactionSynchronizationManager.isSynchronizationActive()) {

                TransactionSynchronizationManager.registerSynchronization(
                    object : TransactionSynchronization {
                        override fun afterCommit() {
                            val counter = counterCache.computeIfAbsent(event.eventType) {
                                Counter.builder("outbox_insert_total")
                                    .description("Total committed outbox inserts")
                                    .tag("eventType", it)
                                    .register(meterRegistry)
                            }
                            counter.increment()
                        }
                    }
                )
            }
        } catch (e: Exception) {
            log.error("[OUTBOX_SAVE_FAILED] aggregateType={}, aggregateId={}, eventType={}, error={}",
                event.aggregateType, event.aggregateId, event.eventType, e.message, e)
            throw e
        }
    }
}
