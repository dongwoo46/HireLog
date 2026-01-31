package com.hirelog.api.common.domain.process

import java.time.LocalDateTime

/**
 * ProcessedEvent
 *
 * 의미:
 * - 특정 이벤트가 특정 consumer group 기준으로
 *   이미 처리되었음을 나타내는 도메인 객체
 *
 * 책임:
 * - Kafka 소비 과정에서의 멱등 처리 상태 표현
 *
 * 제약:
 * - 저장 방식(JPA, Redis 등)을 알지 않는다
 * - 메시징 기술(Kafka)을 알지 않는다
 */
class ProcessedEvent protected constructor(
    val eventId: ProcessedEventId,
    val consumerGroup: String,
    val processedAt: LocalDateTime
) {

    init {
        require(consumerGroup.isNotBlank()) {
            "consumerGroup must not be blank"
        }
    }

    companion object {

        /**
         * 처리 완료 상태 생성
         */
        fun processed(
            eventId: ProcessedEventId,
            consumerGroup: String,
            processedAt: LocalDateTime = LocalDateTime.now()
        ): ProcessedEvent {
            return ProcessedEvent(
                eventId = eventId,
                consumerGroup = consumerGroup,
                processedAt = processedAt
            )
        }
    }
}
