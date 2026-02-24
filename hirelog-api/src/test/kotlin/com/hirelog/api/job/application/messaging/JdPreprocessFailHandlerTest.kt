package com.hirelog.api.job.application.messaging

import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.port.JobSummaryRequestCommand
import com.hirelog.api.job.domain.model.JobSummaryRequest
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus
import io.mockk.*
import org.junit.jupiter.api.*

@DisplayName("JdPreprocessFailHandler 테스트")
class JdPreprocessFailHandlerTest {

    private lateinit var handler: JdPreprocessFailHandler
    private lateinit var processingWriteService: JdSummaryProcessingWriteService
    private lateinit var jobSummaryRequestCommand: JobSummaryRequestCommand

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
        jobSummaryRequestCommand = mockk()
        handler = JdPreprocessFailHandler(processingWriteService, jobSummaryRequestCommand)
    }

    @Nested
    @DisplayName("handle 메서드는")
    inner class HandleTest {

        @Test
        @DisplayName("Processing을 FAILED로 전이하고 PENDING Request도 FAILED로 전이한다")
        fun shouldFailBothProcessingAndRequest() {
            val request = mockk<JobSummaryRequest>(relaxed = true)

            every {
                jobSummaryRequestCommand.findByRequestIdAndStatus(requestId, JobSummaryRequestStatus.PENDING)
            } returns request
            every { jobSummaryRequestCommand.save(request) } returns request

            handler.handle(failEvent)

            verify {
                processingWriteService.markFailed(
                    any(),
                    eq("OCR_FAILED"),
                    eq("OCR processing error")
                )
            }
            verify { request.markFailed() }
            verify { jobSummaryRequestCommand.save(request) }
        }

        @Test
        @DisplayName("PENDING Request가 없으면 Processing만 FAILED로 전이한다")
        fun shouldOnlyFailProcessingWhenRequestNotFound() {
            every {
                jobSummaryRequestCommand.findByRequestIdAndStatus(requestId, JobSummaryRequestStatus.PENDING)
            } returns null

            handler.handle(failEvent)

            verify {
                processingWriteService.markFailed(
                    any(),
                    eq("OCR_FAILED"),
                    eq("OCR processing error")
                )
            }
            verify(exactly = 0) { jobSummaryRequestCommand.save(any()) }
        }
    }
}
