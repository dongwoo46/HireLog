package com.hirelog.api.job.application.summary

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.application.outbox.OutboxEventWriteService
import com.hirelog.api.common.config.properties.LlmProperties
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingCommand
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.domain.model.JobSummary
import io.mockk.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.springframework.context.ApplicationEventPublisher

@DisplayName("JobSummaryWriteService 테스트")
class JobSummaryWriteServiceTest {

    private lateinit var service: JobSummaryWriteService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var summaryCommand: JobSummaryCommand
    private lateinit var outboxEventWriteService: OutboxEventWriteService
    private lateinit var processingCommand: JdSummaryProcessingCommand
    private lateinit var processingQuery: JdSummaryProcessingQuery
    private lateinit var llmProperties: LlmProperties
    private lateinit var eventPublisher: ApplicationEventPublisher

    @BeforeEach
    fun setUp() {
        objectMapper = mockk()
        summaryCommand = mockk()
        outboxEventWriteService = mockk(relaxed = true)
        processingCommand = mockk(relaxed = true)
        processingQuery = mockk()
        llmProperties = mockk()
        eventPublisher = mockk(relaxed = true)

        every { objectMapper.writeValueAsString(any()) } returns "{}"

        service = JobSummaryWriteService(
            objectMapper, summaryCommand, outboxEventWriteService,
            processingCommand, processingQuery, llmProperties, eventPublisher
        )
    }

    @Nested
    @DisplayName("deactivate 메서드는")
    inner class DeactivateTest {

        @Test
        @DisplayName("JobSummary를 비활성화하고 Outbox 이벤트를 발행한다")
        fun shouldDeactivateSummaryAndAppendOutbox() {
            val summary = mockk<JobSummary>(relaxed = true)
            every { summary.id } returns 1L

            every { summaryCommand.findById(1L) } returns summary
            every { summaryCommand.update(summary) } just runs

            service.deactivate(1L)

            verify { summary.deactivate() }
            verify { summaryCommand.update(summary) }
            verify { outboxEventWriteService.append(any()) }
        }

        @Test
        @DisplayName("JobSummary가 없으면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenNotFound() {
            every { summaryCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.deactivate(999L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JobSummary not found")
        }
    }

    @Nested
    @DisplayName("activate 메서드는")
    inner class ActivateTest {

        @Test
        @DisplayName("JobSummary를 활성화하고 Outbox 이벤트를 발행한다")
        fun shouldActivateSummaryAndAppendOutbox() {
            val summary = mockk<JobSummary>(relaxed = true)
            every { summary.id } returns 1L

            every { summaryCommand.findById(1L) } returns summary
            every { summaryCommand.update(summary) } just runs

            service.activate(1L)

            verify { summary.activate() }
            verify { summaryCommand.update(summary) }
            verify { outboxEventWriteService.append(any()) }
        }

        @Test
        @DisplayName("JobSummary가 없으면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenNotFound() {
            every { summaryCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.activate(999L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JobSummary not found")
        }
    }
}
