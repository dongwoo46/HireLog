package com.hirelog.api.common.application.processed

import com.hirelog.api.common.domain.ProcessedEvent
import com.hirelog.api.common.domain.ProcessedEventId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ProcessedEventService
 *
 * 책임:
 * - Kafka 소비 시 멱등 처리 유스케이스 수행
 *
 * 역할:
 * - 이미 처리된 이벤트인지 판단
 * - 신규 이벤트인 경우 처리 완료 상태 기록
 */
@Service
class ProcessedEventService(
    private val processedEventQuery: ProcessedEventQuery,
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

        // 이미 처리된 이벤트인 경우
        if (processedEventQuery.exists(eventId, consumerGroup)) {
            return true
        }

        // 신규 이벤트 → 처리 완료로 기록
        val processedEvent = ProcessedEvent.processed(
            eventId = eventId,
            consumerGroup = consumerGroup
        )

        processedEventCommand.save(processedEvent)
        return false
    }
}
