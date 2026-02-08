package com.hirelog.api.common.application.outbox

import com.hirelog.api.common.domain.outbox.OutboxEvent
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.lang.RuntimeException
@DisplayName("OutboxEventWriteService 테스트")
class OutboxEventWriteServiceTest {

    private val command: OutboxEventCommand = mockk()
    private val service = OutboxEventWriteService(command)

    @Nested
    @DisplayName("append")
    inner class Append {

        @Test
        @DisplayName("정상: OutboxEvent를 저장한다")
        fun should_save_outbox_event() {
            // given
            val event = createOutboxEvent()
            every { command.save(event) } just Runs

            // when
            service.append(event)

            // then
            verify(exactly = 1) { command.save(event) }
        }

        @Test
        @DisplayName("실패: 저장 중 예외가 발생하면 그대로 전파한다")
        fun should_rethrow_exception_when_save_fails() {
            // given
            val event = createOutboxEvent()
            val exception = RuntimeException("DB Error")

            every { command.save(event) } throws exception

            // when & then
            val thrown = assertThrows(RuntimeException::class.java) {
                service.append(event)
            }

            assertEquals("DB Error", thrown.message)
        }




    }

    private fun createOutboxEvent(): OutboxEvent =
        OutboxEvent.occurredWithString(
            aggregateType = "TEST",
            aggregateId = "1",
            eventType = "CREATED",
            payload = "{}"
        )
}
