package com.hirelog.api.job.application.summary

/**
 * JobSummary Outbox 이벤트 상수
 *
 * 주의:
 * - AggregateType은 common/domain/outbox/AggregateType enum 사용
 */
object JobSummaryOutboxConstants {

    /**
     * Event Types
     */
    object EventType {
        const val CREATED = "CREATED"
    }
}
