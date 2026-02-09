package com.hirelog.api.common.domain.outbox

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("OutboxEvent 도메인 테스트")
class OutboxEventTest {

    @Nested
    @DisplayName("occurred 메서드는")
    inner class OccurredTest {

        @Test
        @DisplayName("AggregateType enum으로 Outbox 이벤트를 생성한다")
        fun shouldCreateOutboxEventWithAggregateType() {
            // given
            val aggregateType = AggregateType.JOB_SUMMARY
            val aggregateId = "123"
            val eventType = "MEMBER_CREATED"
            val payload = """{"id":123,"name":"test"}"""

            val beforeOccurred = LocalDateTime.now()

            // when
            val event = OutboxEvent.occurred(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload
            )

            // then
            assertThat(event.id).isNotNull()
            assertThat(event.aggregateType).isEqualTo(aggregateType.value)
            assertThat(event.aggregateId).isEqualTo(aggregateId)
            assertThat(event.eventType).isEqualTo(eventType)
            assertThat(event.payload).isEqualTo(payload)
            assertThat(event.occurredAt).isAfterOrEqualTo(beforeOccurred)
        }

        @Test
        @DisplayName("각 호출마다 고유한 UUID를 생성한다")
        fun shouldGenerateUniqueUUID() {
            // given
            val aggregateType = AggregateType.JOB_SUMMARY
            val aggregateId = "123"
            val eventType = "MEMBER_CREATED"
            val payload = "{}"

            // when
            val event1 = OutboxEvent.occurred(aggregateType, aggregateId, eventType, payload)
            val event2 = OutboxEvent.occurred(aggregateType, aggregateId, eventType, payload)
            val event3 = OutboxEvent.occurred(aggregateType, aggregateId, eventType, payload)

            // then
            assertThat(setOf(event1.id, event2.id, event3.id)).hasSize(3)
        }



        @Test
        @DisplayName("빈 payload도 허용한다")
        fun shouldAllowEmptyPayload() {
            // given
            val aggregateType = AggregateType.JOB_SUMMARY
            val aggregateId = "123"
            val eventType = "MEMBER_DELETED"
            val payload = ""

            // when
            val event = OutboxEvent.occurred(aggregateType, aggregateId, eventType, payload)

            // then
            assertThat(event.payload).isEmpty()
        }
    }

    @Nested
    @DisplayName("occurredWithString 메서드는")
    inner class OccurredWithStringTest {

        @Test
        @DisplayName("String으로 Outbox 이벤트를 생성한다")
        fun shouldCreateOutboxEventWithString() {
            // given
            val aggregateType = "MEMBER"
            val aggregateId = "123"
            val eventType = "MEMBER_CREATED"
            val payload = """{"id":123}"""

            // when
            @Suppress("DEPRECATION")
            val event = OutboxEvent.occurredWithString(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload
            )

            // then
            assertThat(event.aggregateType).isEqualTo(aggregateType)
            assertThat(event.aggregateId).isEqualTo(aggregateId)
            assertThat(event.eventType).isEqualTo(eventType)
            assertThat(event.payload).isEqualTo(payload)
        }

        @Test
        @DisplayName("커스텀 aggregateType 문자열도 허용한다")
        fun shouldAllowCustomAggregateTypeString() {
            // given
            val customAggregateType = "CUSTOM_AGGREGATE"

            // when
            @Suppress("DEPRECATION")
            val event = OutboxEvent.occurredWithString(
                aggregateType = customAggregateType,
                aggregateId = "1",
                eventType = "CUSTOM_EVENT",
                payload = "{}"
            )

            // then
            assertThat(event.aggregateType).isEqualTo(customAggregateType)
        }
    }

    @Nested
    @DisplayName("생성자 검증은")
    inner class ConstructorValidationTest {

        @Test
        @DisplayName("aggregateType이 빈 문자열이면 예외를 발생시킨다")
        fun shouldThrowExceptionWhenAggregateTypeIsBlank() {
            // when & then
            assertThatThrownBy {
                @Suppress("DEPRECATION")
                OutboxEvent.occurredWithString(
                    aggregateType = "",
                    aggregateId = "123",
                    eventType = "EVENT",
                    payload = "{}"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("aggregateType이 공백 문자열이면 예외를 발생시킨다")
        fun shouldThrowExceptionWhenAggregateTypeIsWhitespace() {
            // when & then
            assertThatThrownBy {
                @Suppress("DEPRECATION")
                OutboxEvent.occurredWithString(
                    aggregateType = "   ",
                    aggregateId = "123",
                    eventType = "EVENT",
                    payload = "{}"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("aggregateId가 빈 문자열이면 예외를 발생시킨다")
        fun shouldThrowExceptionWhenAggregateIdIsBlank() {
            // when & then
            assertThatThrownBy {
                OutboxEvent.occurred(
                    aggregateType = AggregateType.JOB_SUMMARY,
                    aggregateId = "",
                    eventType = "EVENT",
                    payload = "{}"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("aggregateId가 공백 문자열이면 예외를 발생시킨다")
        fun shouldThrowExceptionWhenAggregateIdIsWhitespace() {
            // when & then
            assertThatThrownBy {
                OutboxEvent.occurred(
                    aggregateType = AggregateType.JOB_SUMMARY,
                    aggregateId = "   ",
                    eventType = "EVENT",
                    payload = "{}"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("eventType이 빈 문자열이면 예외를 발생시킨다")
        fun shouldThrowExceptionWhenEventTypeIsBlank() {
            // when & then
            assertThatThrownBy {
                OutboxEvent.occurred(
                    aggregateType = AggregateType.JOB_SUMMARY,
                    aggregateId = "123",
                    eventType = "",
                    payload = "{}"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("eventType이 공백 문자열이면 예외를 발생시킨다")
        fun shouldThrowExceptionWhenEventTypeIsWhitespace() {
            // when & then
            assertThatThrownBy {
                OutboxEvent.occurred(
                    aggregateType = AggregateType.JOB_SUMMARY,
                    aggregateId = "123",
                    eventType = "   ",
                    payload = "{}"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}