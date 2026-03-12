package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.event.JobSummaryRequestEvent
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

@DisplayName("PipelineErrorHandler 테스트")
class PipelineErrorHandlerTest {

    private lateinit var handler: PipelineErrorHandler
    private lateinit var processingWriteService: JdSummaryProcessingWriteService
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val processingId = UUID.randomUUID()
    private val requestId = "req-001"
    private val phase = "POST_LLM"

    @BeforeEach
    fun setUp() {
        processingWriteService = mockk(relaxed = true)
        eventPublisher = mockk(relaxed = true)
        handler = PipelineErrorHandler(processingWriteService, eventPublisher)
    }

    @Nested
    @DisplayName("handle 메서드는")
    inner class HandleTest {

        @Test
        @DisplayName("GeminiCallException → errorCode=LLM_CALL_FAILED")
        fun shouldHandleGeminiCallException() {
            handler.handle(processingId, GeminiCallException(RuntimeException("api error")), requestId, phase)

            verify {
                processingWriteService.markFailed(processingId, "LLM_CALL_FAILED", any())
            }
            verifyPublishedFailedEvent("LLM_CALL_FAILED", retryable = true)
        }

        @Test
        @DisplayName("GeminiParseException → errorCode=LLM_PARSE_FAILED")
        fun shouldHandleGeminiParseException() {
            handler.handle(processingId, GeminiParseException(RuntimeException("parse error")), requestId, phase)

            verify {
                processingWriteService.markFailed(processingId, "LLM_PARSE_FAILED", any())
            }
            verifyPublishedFailedEvent("LLM_PARSE_FAILED", retryable = false)
        }

        @Test
        @DisplayName("TimeoutException → errorCode=LLM_TIMEOUT")
        fun shouldHandleTimeoutException() {
            handler.handle(processingId, TimeoutException("timeout"), requestId, phase)

            verify {
                processingWriteService.markFailed(processingId, "LLM_TIMEOUT", any())
            }
            verifyPublishedFailedEvent("LLM_TIMEOUT", retryable = true)
        }

        @Test
        @DisplayName("일반 예외 → errorCode=FAILED_AT_<phase>")
        fun shouldHandleGenericException() {
            handler.handle(processingId, RuntimeException("unknown"), requestId, phase)

            verify {
                processingWriteService.markFailed(processingId, "FAILED_AT_POST_LLM", any())
            }
            verifyPublishedFailedEvent("FAILED_AT_POST_LLM", retryable = false)
        }

        @Test
        @DisplayName("CompletionException 래핑된 TimeoutException → cause 기준으로 errorCode 결정")
        fun shouldUnwrapCompletionException() {
            val wrapped = CompletionException(TimeoutException("wrapped timeout"))

            handler.handle(processingId, wrapped, requestId, phase)

            verify {
                processingWriteService.markFailed(processingId, "LLM_TIMEOUT", any())
            }
        }

        private fun verifyPublishedFailedEvent(expectedErrorCode: String, retryable: Boolean) {
            val slot = slot<JobSummaryRequestEvent.Failed>()
            verify { eventPublisher.publishEvent(capture(slot)) }

            assertThat(slot.captured.errorCode).isEqualTo(expectedErrorCode)
            assertThat(slot.captured.retryable).isEqualTo(retryable)
            assertThat(slot.captured.requestId).isEqualTo(requestId)
        }
    }
}
