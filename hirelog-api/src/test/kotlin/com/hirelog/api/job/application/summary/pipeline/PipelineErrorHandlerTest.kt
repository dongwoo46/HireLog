package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.event.JobSummaryRequestEvent
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.github.resilience4j.ratelimiter.RateLimiter
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

@DisplayName("PipelineErrorHandler test")
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
    @DisplayName("handle")
    inner class HandleTest {

        @Test
        @DisplayName("GeminiCallException -> LLM_CALL_FAILED")
        fun shouldHandleGeminiCallExceptionAsCallFailed() {
            handler.handle(processingId, GeminiCallException(RuntimeException("api error")), requestId, phase)

            verify {
                processingWriteService.markFailed(processingId, "LLM_CALL_FAILED", any())
            }
            verifyPublishedFailedEvent("LLM_CALL_FAILED", retryable = true)
        }

        @Test
        @DisplayName("GeminiCallException(RequestNotPermitted) -> LLM_RATE_LIMITED")
        fun shouldHandleGeminiCallExceptionAsRateLimited() {
            handler.handle(
                processingId,
                GeminiCallException(RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("test-rate"))),
                requestId,
                phase
            )

            verify {
                processingWriteService.markFailed(processingId, "LLM_RATE_LIMITED", any())
            }
            verifyPublishedFailedEvent("LLM_RATE_LIMITED", retryable = true)
        }

        @Test
        @DisplayName("GeminiCallException(CallNotPermittedException) -> LLM_CIRCUIT_OPEN")
        fun shouldHandleGeminiCallExceptionAsCircuitOpen() {
            handler.handle(
                processingId,
                GeminiCallException(CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("test-cb"))),
                requestId,
                phase
            )

            verify {
                processingWriteService.markFailed(processingId, "LLM_CIRCUIT_OPEN", any())
            }
            verifyPublishedFailedEvent("LLM_CIRCUIT_OPEN", retryable = true)
        }

        @Test
        @DisplayName("GeminiParseException -> LLM_PARSE_FAILED")
        fun shouldHandleGeminiParseException() {
            handler.handle(processingId, GeminiParseException(RuntimeException("parse error")), requestId, phase)

            verify {
                processingWriteService.markFailed(processingId, "LLM_PARSE_FAILED", any())
            }
            verifyPublishedFailedEvent("LLM_PARSE_FAILED", retryable = false)
        }

        @Test
        @DisplayName("TimeoutException -> LLM_TIMEOUT")
        fun shouldHandleTimeoutException() {
            handler.handle(processingId, TimeoutException("timeout"), requestId, phase)

            verify {
                processingWriteService.markFailed(processingId, "LLM_TIMEOUT", any())
            }
            verifyPublishedFailedEvent("LLM_TIMEOUT", retryable = true)
        }

        @Test
        @DisplayName("Generic exception -> FAILED_AT_<phase>")
        fun shouldHandleGenericException() {
            handler.handle(processingId, RuntimeException("unknown"), requestId, phase)

            verify {
                processingWriteService.markFailed(processingId, "FAILED_AT_POST_LLM", any())
            }
            verifyPublishedFailedEvent("FAILED_AT_POST_LLM", retryable = false)
        }

        @Test
        @DisplayName("CompletionException(timeout) should be unwrapped")
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
