package com.hirelog.api.common.application.processed

import com.hirelog.api.common.domain.process.ProcessedEventId

/**
 * ProcessedEventQuery
 *
 * 책임:
 * - 이벤트 처리 여부 조회
 */
interface ProcessedEventQuery {

    /**
     * 특정 이벤트가 해당 consumer group 기준으로
     * 이미 처리되었는지 확인
     */
    fun exists(
        eventId: ProcessedEventId,
        consumerGroup: String
    ): Boolean
}
