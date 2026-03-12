package com.hirelog.api.job.application.jobsummaryprocessing

import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingCommand
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.domain.model.JdSummaryProcessing
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.util.UUID

@DisplayName("JdSummaryProcessingWriteService 테스트")
class JdSummaryProcessingWriteServiceTest {

    private lateinit var service: JdSummaryProcessingWriteService
    private lateinit var command: JdSummaryProcessingCommand
    private lateinit var query: JdSummaryProcessingQuery

    private val processingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        command = mockk(relaxed = true)
        query = mockk()
        service = JdSummaryProcessingWriteService(command, query)
    }

    @Nested
    @DisplayName("startProcessing 메서드는")
    inner class StartProcessingTest {

        @Test
        @DisplayName("Processing을 생성하고 저장 후 반환한다")
        fun shouldCreateAndSaveProcessing() {
            val requestId = processingId.toString()
            every { command.save(any()) } returnsArgument 0

            val result = service.startProcessing(requestId)

            assertThat(result).isNotNull()
            assertThat(result.id).isEqualTo(processingId)
            verify(exactly = 1) { command.save(any()) }
        }
    }

    @Nested
    @DisplayName("markSummarizing 메서드는")
    inner class MarkSummarizingTest {

        @Test
        @DisplayName("Processing을 SUMMARIZING 상태로 전이한다")
        fun shouldMarkSummarizing() {
            val processing = mockk<JdSummaryProcessing>(relaxed = true)
            every { query.findById(processingId) } returns processing

            service.markSummarizing(processingId, 100L)

            verify { processing.markSummarizing(100L) }
            verify { command.update(processing) }
        }

        @Test
        @DisplayName("Processing이 없으면 IllegalStateException을 던진다")
        fun shouldThrowWhenNotFound() {
            every { query.findById(processingId) } returns null

            assertThatThrownBy {
                service.markSummarizing(processingId, 100L)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("not found")
        }
    }

    @Nested
    @DisplayName("saveLlmResult 메서드는")
    inner class SaveLlmResultTest {

        @Test
        @DisplayName("LLM 결과를 임시 저장한다")
        fun shouldSaveLlmResult() {
            val processing = mockk<JdSummaryProcessing>(relaxed = true)
            every { query.findById(processingId) } returns processing

            service.saveLlmResult(processingId, """{"result":"ok"}""", "Toss", "Backend Engineer")

            verify { processing.saveLlmResult("""{"result":"ok"}""", "Toss", "Backend Engineer") }
            verify { command.update(processing) }
        }

        @Test
        @DisplayName("Processing이 없으면 IllegalStateException을 던진다")
        fun shouldThrowWhenNotFound() {
            every { query.findById(processingId) } returns null

            assertThatThrownBy {
                service.saveLlmResult(processingId, """{}""", "Brand", "Position")
            }.isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Nested
    @DisplayName("markDuplicate 메서드는")
    inner class MarkDuplicateTest {

        @Test
        @DisplayName("Processing을 DUPLICATE 상태로 전이한다")
        fun shouldMarkDuplicate() {
            val processing = mockk<JdSummaryProcessing>(relaxed = true)
            every { query.findById(processingId) } returns processing

            service.markDuplicate(processingId, "HASH_DUPLICATE")

            verify { processing.markDuplicate("HASH_DUPLICATE") }
            verify { command.update(processing) }
        }
    }

    @Nested
    @DisplayName("markCompleted 메서드는")
    inner class MarkCompletedTest {

        @Test
        @DisplayName("Processing을 COMPLETED 상태로 전이한다")
        fun shouldMarkCompleted() {
            val processing = mockk<JdSummaryProcessing>(relaxed = true)
            every { query.findById(processingId) } returns processing

            service.markCompleted(processingId, 999L)

            verify { processing.markCompleted(999L) }
            verify { command.update(processing) }
        }
    }

    @Nested
    @DisplayName("markFailed 메서드는")
    inner class MarkFailedTest {

        @Test
        @DisplayName("Processing을 FAILED 상태로 전이한다")
        fun shouldMarkFailed() {
            val processing = mockk<JdSummaryProcessing>(relaxed = true)
            every { query.findById(processingId) } returns processing

            service.markFailed(processingId, "LLM_TIMEOUT", "Request timed out")

            verify { processing.markFailed("LLM_TIMEOUT", "Request timed out") }
            verify { command.update(processing) }
        }

        @Test
        @DisplayName("Processing이 없으면 IllegalStateException을 던진다")
        fun shouldThrowWhenNotFound() {
            every { query.findById(processingId) } returns null

            assertThatThrownBy {
                service.markFailed(processingId, "ERROR", "msg")
            }.isInstanceOf(IllegalStateException::class.java)
        }
    }
}
