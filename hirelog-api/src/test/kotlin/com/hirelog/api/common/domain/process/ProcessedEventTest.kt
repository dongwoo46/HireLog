package com.hirelog.api.common.domain.process

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("ProcessedEvent 도메인 테스트")
class ProcessedEventTest {

    @Nested
    @DisplayName("processed 메서드는")
    inner class ProcessedTest {

        @Test
        @DisplayName("처리 완료 이벤트를 생성한다")
        fun shouldCreateProcessedEvent() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")

            val consumerGroup = "member-service-group"
            val processedAt = LocalDateTime.now()

            // when
            val event = ProcessedEvent.processed(
                eventId = eventId,
                consumerGroup = consumerGroup,
                processedAt = processedAt
            )

            // then
            assertThat(event.eventId).isEqualTo(eventId)
            assertThat(event.consumerGroup).isEqualTo(consumerGroup)
            assertThat(event.processedAt).isEqualTo(processedAt)
        }

        @Test
        @DisplayName("processedAt을 생략하면 현재 시간으로 설정된다")
        fun shouldUseCurrentTimeWhenProcessedAtIsOmitted() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")

            val consumerGroup = "brand-service-group"
            val beforeCreation = LocalDateTime.now()

            // when
            val event = ProcessedEvent.processed(
                eventId = eventId,
                consumerGroup = consumerGroup
            )

            // then
            assertThat(event.processedAt).isAfterOrEqualTo(beforeCreation)
        }

        @Test
        @DisplayName("다양한 consumer group으로 이벤트를 생성할 수 있다")
        fun shouldCreateEventsWithDifferentConsumerGroups() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")

            val consumerGroups = listOf(
                "notification-service",
                "analytics-service",
                "audit-service"
            )

            // when & then
            consumerGroups.forEach { consumerGroup ->
                val event = ProcessedEvent.processed(
                    eventId = eventId,
                    consumerGroup = consumerGroup
                )

                assertThat(event.consumerGroup).isEqualTo(consumerGroup)
            }
        }

        @Test
        @DisplayName("동일한 eventId로 여러 consumer group의 처리 기록을 생성할 수 있다")
        fun shouldCreateMultipleProcessedEventsForSameEventId() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")


            // when
            val event1 = ProcessedEvent.processed(eventId, "group-1")
            val event2 = ProcessedEvent.processed(eventId, "group-2")
            val event3 = ProcessedEvent.processed(eventId, "group-3")

            // then
            assertThat(event1.eventId).isEqualTo(eventId)
            assertThat(event2.eventId).isEqualTo(eventId)
            assertThat(event3.eventId).isEqualTo(eventId)
            assertThat(event1.consumerGroup).isNotEqualTo(event2.consumerGroup)
            assertThat(event2.consumerGroup).isNotEqualTo(event3.consumerGroup)
        }
    }

    @Nested
    @DisplayName("생성자 검증은")
    inner class ConstructorValidationTest {

        @Test
        @DisplayName("consumerGroup이 빈 문자열이면 예외를 발생시킨다")
        fun shouldThrowExceptionWhenConsumerGroupIsBlank() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")


            // when & then
            assertThatThrownBy {
                ProcessedEvent.processed(
                    eventId = eventId,
                    consumerGroup = ""
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("consumerGroup must not be blank")
        }

        @Test
        @DisplayName("consumerGroup이 공백 문자열이면 예외를 발생시킨다")
        fun shouldThrowExceptionWhenConsumerGroupIsWhitespace() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")


            // when & then
            assertThatThrownBy {
                ProcessedEvent.processed(
                    eventId = eventId,
                    consumerGroup = "   "
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("consumerGroup must not be blank")
        }

        @Test
        @DisplayName("유효한 consumerGroup은 검증을 통과한다")
        fun shouldPassValidationWithValidConsumerGroup() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")

            val validGroups = listOf(
                "service-group",
                "consumer-1",
                "analytics_service",
                "notification.service"
            )

            // when & then
            validGroups.forEach { group ->
                val event = ProcessedEvent.processed(
                    eventId = eventId,
                    consumerGroup = group
                )

                assertThat(event.consumerGroup).isEqualTo(group)
            }
        }
    }

    @Nested
    @DisplayName("멱등성 시나리오 테스트")
    inner class IdempotencyScenarioTest {

        @Test
        @DisplayName("동일한 이벤트가 다른 시간에 처리되어도 eventId는 동일하다")
        fun shouldHaveSameEventIdForSameEventProcessedAtDifferentTimes() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")

            val consumerGroup = "service-group"

            // when
            val event1 = ProcessedEvent.processed(
                eventId = eventId,
                consumerGroup = consumerGroup,
                processedAt = LocalDateTime.of(2024, 1, 1, 10, 0)
            )
            val event2 = ProcessedEvent.processed(
                eventId = eventId,
                consumerGroup = consumerGroup,
                processedAt = LocalDateTime.of(2024, 1, 1, 11, 0)
            )

            // then
            assertThat(event1.eventId).isEqualTo(event2.eventId)
            assertThat(event1.consumerGroup).isEqualTo(event2.consumerGroup)
            assertThat(event1.processedAt).isBefore(event2.processedAt)
        }
    }
}