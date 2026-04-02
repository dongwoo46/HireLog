package com.hirelog.api.job.application.messaging

import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.domain.model.JdSummaryProcessing
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID

@DisplayName("JdPreprocessFailHandler 테스트")
class JdPreprocessFailHandlerTest {

    private lateinit var handler: JdPreprocessFailHandler
    private lateinit var processingWriteService: JdSummaryProcessingWriteService
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val requestId = "550e8400-e29b-41d4-a716-446655440000"

    private val failEvent = JdPreprocessFailEvent(
        eventId = "evt-001",
        requestId = requestId,
        eventType = "JD_PREPROCESS_FAILED",
        version = "v1",
        occurredAt = System.currentTimeMillis(),
        source = "TEXT",
        errorCode = "OCR_FAILED",
        errorMessage = "OCR processing error",
        errorCategory = "RECOVERABLE",
        pipelineStage = "OCR",
        workerHost = "worker-01",
        processingDurationMs = 3000,
        kafkaMetadata = null
    )

    @BeforeEach
    fun setUp() {
        processingWriteService = mockk(relaxed = true)
        eventPublisher = mockk(relaxed = true)
        handler = JdPreprocessFailHandler(processingWriteService, eventPublisher)
    }

    @Nested
    @DisplayName("handle 메서드는")
    inner class HandleTest {

        @Test
        @DisplayName("Processing을 FAILED로 전이하고 실패 이벤트를 발행한다")
        fun shouldFailProcessingAndPublishEvent() {
            val processing = JdSummaryProcessing.create(
                id = UUID.fromString(requestId),
                brandName = "Toss",
                positionName = "Backend Engineer"
            )
            every {
                processingWriteService.markFailed(
                    any(),
                    any(),
                    any()
                )
            } returns processing

            handler.handle(failEvent)

            verify {
                processingWriteService.markFailed(
                    any(),
                    "OCR_FAILED",
                    "OCR processing error"
                )
            }
            verify { eventPublisher.publishEvent(any()) }
        }
    }
}
