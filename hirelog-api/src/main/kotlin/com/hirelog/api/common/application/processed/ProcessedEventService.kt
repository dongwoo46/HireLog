package com.hirelog.api.common.application.processed

import com.hirelog.api.common.domain.process.ProcessedEvent
import com.hirelog.api.common.domain.process.ProcessedEventId
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ProcessedEventService
 *
 * 책임:
 * - Kafka 소비 시 멱등 처리 유스케이스 수행
 *
 * 정책:
 * - DB unique constraint를 최종 멱등성 방어선으로 사용
 */
@Service
class ProcessedEventService(
    private val processedEventCommand: ProcessedEventCommand
) {

    /**
     * 이벤트 멱등 처리 수행
     *
     * @return true  이미 처리된 이벤트
     * @return false 최초 처리 이벤트
     */
    @Transactional
    fun isAlreadyProcessedOrMark(
        eventId: ProcessedEventId,
        consumerGroup: String
    ): Boolean {
        return try {
            val processedEvent = ProcessedEvent.processed(
                eventId = eventId,
                consumerGroup = consumerGroup
            )

            processedEventCommand.save(processedEvent)

            // insert 성공 → 최초 처리
            false
        } catch (e: DataIntegrityViolationException) {
            // unique constraint 위반 → 이미 처리됨
            true
        }
    }
}
