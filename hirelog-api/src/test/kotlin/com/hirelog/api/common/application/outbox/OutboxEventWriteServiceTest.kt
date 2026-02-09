package com.hirelog.api.common.application.outbox

import com.hirelog.api.common.domain.outbox.AggregateType
import com.hirelog.api.common.domain.outbox.OutboxEvent
import io.mockk.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OutboxEventWriteService 테스트")
class OutboxEventWriteServiceTest {

    private lateinit var outboxEventWriteService: OutboxEventWriteService
    private lateinit var outboxEventCommand: OutboxEventCommand

    @BeforeEach
    fun setUp() {
        outboxEventCommand = mockk()
        outboxEventWriteService = OutboxEventWriteService(outboxEventCommand)
    }

    @Nested
    @DisplayName("append 메서드는")
    inner class AppendTest {

        @Test
        @DisplayName("Outbox 이벤트를 저장한다")
        fun shouldAppendOutboxEvent() {
            // given
            val event = OutboxEvent.occurred(
                aggregateType = AggregateType.JOB_SUMMARY,
                aggregateId = "123",
                eventType = "MEMBER_CREATED",
                payload = """{"id":123,"name":"test"}"""
            )

            every { outboxEventCommand.save(event) } just Runs

            // when
            outboxEventWriteService.append(event)

            // then
            verify(exactly = 1) { outboxEventCommand.save(event) }
        }



        @Test
        @DisplayName("저장 실패 시 예외를 던진다")
        fun shouldThrowExceptionWhenSaveFails() {
            // given
            val event = OutboxEvent.occurred(
                aggregateType = AggregateType.JOB_SUMMARY,
                aggregateId = "123",
                eventType = "MEMBER_CREATED",
                payload = "{}"
            )

            val exception = RuntimeException("Database connection failed")
            every { outboxEventCommand.save(event) } throws exception

            // when & then
            assertThatThrownBy {
                outboxEventWriteService.append(event)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Database connection failed")

            verify(exactly = 1) { outboxEventCommand.save(event) }
        }

        @Test
        @DisplayName("저장 실패 시에도 예외를 재throw한다")
        fun shouldRethrowExceptionOnFailure() {
            // given
            val event = OutboxEvent.occurred(
                aggregateType = AggregateType.JOB_SUMMARY,
                aggregateId = "456",
                eventType = "BRAND_REJECTED",
                payload = "{}"
            )

            val exception = IllegalStateException("Invalid state")
            every { outboxEventCommand.save(event) } throws exception

            // when & then
            assertThatThrownBy {
                outboxEventWriteService.append(event)
            }
                .isSameAs(exception)
        }
    }
}