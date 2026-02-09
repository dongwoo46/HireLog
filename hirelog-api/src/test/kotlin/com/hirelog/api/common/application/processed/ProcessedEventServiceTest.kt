package com.hirelog.api.common.application.processed

import com.hirelog.api.common.domain.process.ProcessedEvent
import com.hirelog.api.common.domain.process.ProcessedEventId
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("ProcessedEventService 테스트")
class ProcessedEventServiceTest {

    private lateinit var processedEventService: ProcessedEventService
    private lateinit var processedEventCommand: ProcessedEventCommand

    @BeforeEach
    fun setUp() {
        processedEventCommand = mockk()
        processedEventService = ProcessedEventService(processedEventCommand)
    }

    @Nested
    @DisplayName("isAlreadyProcessedOrMark 메서드는")
    inner class IsAlreadyProcessedOrMarkTest {

        @Test
        @DisplayName("최초 처리 이벤트는 false를 반환한다")
        fun shouldReturnFalseForFirstTimeProcessing() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")
            val consumerGroup = "member-service-group"

            every { processedEventCommand.save(any()) } just Runs

            // when
            val result = processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup)

            // then
            assertThat(result).isFalse()
            verify(exactly = 1) { processedEventCommand.save(any()) }
        }

        @Test
        @DisplayName("이미 처리된 이벤트는 true를 반환한다")
        fun shouldReturnTrueForAlreadyProcessedEvent() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")
            val consumerGroup = "member-service-group"

            every { processedEventCommand.save(any()) } throws DataIntegrityViolationException("Duplicate key")

            // when
            val result = processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup)

            // then
            assertThat(result).isTrue()
            verify(exactly = 1) { processedEventCommand.save(any()) }
        }

        @Test
        @DisplayName("동일한 이벤트를 다른 consumer group에서 처리하면 false를 반환한다")
        fun shouldReturnFalseForSameEventWithDifferentConsumerGroup() {
            // given
            val eventId = ProcessedEventId.create("BRAND:456:BRAND_VERIFIED")
            val consumerGroup1 = "notification-service"
            val consumerGroup2 = "analytics-service"

            every { processedEventCommand.save(any()) } just Runs

            // when
            val result1 = processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup1)
            val result2 = processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup2)

            // then
            assertThat(result1).isFalse()
            assertThat(result2).isFalse()
            verify(exactly = 2) { processedEventCommand.save(any()) }
        }

        @Test
        @DisplayName("동일한 eventId와 consumerGroup으로 재시도하면 true를 반환한다")
        fun shouldReturnTrueOnRetryWithSameEventIdAndConsumerGroup() {
            // given
            val eventId = ProcessedEventId.create("COMPANY:789:COMPANY_UPDATED")
            val consumerGroup = "audit-service"

            every { processedEventCommand.save(any()) } answers {
                // 첫 번째 호출
                Unit
            } andThenThrows DataIntegrityViolationException("Duplicate key")

            // when
            val firstResult = processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup)
            val secondResult = processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup)

            // then
            assertThat(firstResult).isFalse() // 최초 처리
            assertThat(secondResult).isTrue() // 중복 처리
            verify(exactly = 2) { processedEventCommand.save(any()) }
        }

        @Test
        @DisplayName("다양한 eventId로 처리 시 각각 false를 반환한다")
        fun shouldReturnFalseForDifferentEventIds() {
            // given
            val eventIds = listOf(
                ProcessedEventId.create("MEMBER:1:CREATED"),
                ProcessedEventId.create("MEMBER:2:CREATED"),
                ProcessedEventId.create("BRAND:3:VERIFIED")
            )
            val consumerGroup = "test-service"

            every { processedEventCommand.save(any()) } just Runs

            // when & then
            eventIds.forEach { eventId ->
                val result = processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup)
                assertThat(result).isFalse()
            }

            verify(exactly = 3) { processedEventCommand.save(any()) }
        }

        @Test
        @DisplayName("ProcessedEvent 객체를 올바르게 생성하여 저장한다")
        fun shouldSaveProcessedEventCorrectly() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:MEMBER_CREATED")
            val consumerGroup = "member-service-group"

            val capturedEvent = slot<ProcessedEvent>()
            every { processedEventCommand.save(capture(capturedEvent)) } just Runs

            // when
            processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup)

            // then
            assertThat(capturedEvent.captured.eventId).isEqualTo(eventId)
            assertThat(capturedEvent.captured.consumerGroup).isEqualTo(consumerGroup)
            assertThat(capturedEvent.captured.processedAt).isNotNull()
        }
    }
}