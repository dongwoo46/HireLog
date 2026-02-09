package com.hirelog.api.common.domain.process

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("ProcessedEvent 도메인 테스트")
class ProcessedEventTest {

    @Nested
    @DisplayName("생성 테스트")
    inner class CreateTest {

        @Test
        @DisplayName("processed: 올바른 값으로 생성되어야 한다")
        fun create_success() {
            // given
            val eventId = ProcessedEventId.create("msg-1")
            val consumerGroup = "group-1"
            val now = LocalDateTime.now()

            // when
            val event = ProcessedEvent.processed(eventId, consumerGroup, now)

            // then
            assertEquals(eventId, event.eventId)
            assertEquals(consumerGroup, event.consumerGroup)
            assertEquals(now, event.processedAt)
        }

        @Test
        @DisplayName("consumerGroup이 공백이면 예외가 발생해야 한다 (init 블록 검증)")
        fun fail_when_blank_consumer_group() {
            // given
            val eventId = ProcessedEventId.create("msg-1")

            // when & then
            val ex = assertThrows(IllegalArgumentException::class.java) {
                ProcessedEvent.processed(eventId, "")
            }
            assertEquals("consumerGroup must not be blank", ex.message)
        }
    }
}
