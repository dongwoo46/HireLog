package com.hirelog.api.common.application.processed

import com.hirelog.api.common.domain.process.ProcessedEventId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("ProcessedEventService 테스트")
class ProcessedEventServiceTest {

    private val command: ProcessedEventCommand = mockk()
    private val service = ProcessedEventService(command)

    @Nested
    @DisplayName("isAlreadyProcessedOrMark")
    inner class IdempotencyTest {

        @Test
        @DisplayName("최초 처리: 저장 성공 시 false를 반환한다")
        fun first_processing() {
            // given
            every { command.save(any()) } returns Unit

            val eventId = ProcessedEventId.create("msg-1")

            // when
            val result = service.isAlreadyProcessedOrMark(
                eventId = eventId,
                consumerGroup = "group-1"
            )

            // then
            assertFalse(result)
            verify(exactly = 1) { command.save(any()) }
        }

        @Test
        @DisplayName("이미 처리됨: 중복 저장 시 true를 반환한다")
        fun already_processed() {
            // given
            every { command.save(any()) } throws DataIntegrityViolationException("Duplicate")

            val eventId = ProcessedEventId.create("msg-1")

            // when
            val result = service.isAlreadyProcessedOrMark(
                eventId = eventId,
                consumerGroup = "group-1"
            )

            // then
            assertTrue(result)
            verify(exactly = 1) { command.save(any()) }
        }
    }
}
