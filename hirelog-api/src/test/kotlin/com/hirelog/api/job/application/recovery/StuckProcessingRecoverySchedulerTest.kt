package com.hirelog.api.job.application.recovery

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.brand.application.BrandWriteService
import com.hirelog.api.common.application.sse.SseEmitterManager
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingCommand
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.application.summary.JobSummaryRequestWriteService
import com.hirelog.api.job.application.summary.JobSummaryWriteService
import com.hirelog.api.job.domain.model.JdSummaryProcessing
import com.hirelog.api.job.domain.type.JdSummaryProcessingStatus
import com.hirelog.api.notification.application.NotificationWriteService
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.util.UUID

@DisplayName("StuckProcessingRecoveryScheduler 테스트")
class StuckProcessingRecoverySchedulerTest {

    private lateinit var scheduler: StuckProcessingRecoveryScheduler
    private lateinit var processingQuery: JdSummaryProcessingQuery
    private lateinit var processingCommand: JdSummaryProcessingCommand
    private lateinit var summaryWriteService: JobSummaryWriteService
    private lateinit var brandWriteService: BrandWriteService
    private lateinit var brandPositionWriteService: BrandPositionWriteService
    private lateinit var positionCommand: PositionCommand
    private lateinit var objectMapper: ObjectMapper
    private lateinit var jobSummaryRequestWriteService: JobSummaryRequestWriteService
    private lateinit var notificationWriteService: NotificationWriteService
    private lateinit var sseEmitterManager: SseEmitterManager

    @BeforeEach
    fun setUp() {
        processingQuery = mockk()
        processingCommand = mockk(relaxed = true)
        summaryWriteService = mockk(relaxed = true)
        brandWriteService = mockk(relaxed = true)
        brandPositionWriteService = mockk(relaxed = true)
        positionCommand = mockk(relaxed = true)
        objectMapper = mockk(relaxed = true)
        jobSummaryRequestWriteService = mockk(relaxed = true)
        notificationWriteService = mockk(relaxed = true)
        sseEmitterManager = mockk(relaxed = true)

        scheduler = StuckProcessingRecoveryScheduler(
            processingQuery, processingCommand, summaryWriteService,
            brandWriteService, brandPositionWriteService, positionCommand,
            objectMapper, jobSummaryRequestWriteService, notificationWriteService, sseEmitterManager
        )
    }

    @Nested
    @DisplayName("recoverStuckProcessing은")
    inner class RecoverStuckProcessingTest {

        @Test
        @DisplayName("Stuck Processing이 없으면 아무것도 하지 않는다")
        fun shouldDoNothingWhenNoStuck() {
            every {
                processingQuery.findStuckWithLlmResult(any(), any(), any())
            } returns emptyList()

            scheduler.recoverStuckProcessing()

            verify(exactly = 0) { processingCommand.update(any()) }
        }
    }

    @Nested
    @DisplayName("markAsFailedIfExhausted는")
    inner class MarkAsFailedIfExhaustedTest {

        @Test
        @DisplayName("processing을 FAILED로 마킹하고 update한다")
        fun shouldMarkFailedAndUpdate() {
            val processingId = UUID.randomUUID()
            val processing = mockk<JdSummaryProcessing>(relaxed = true)
            every { processing.id } returns processingId

            every { jobSummaryRequestWriteService.failRequest(processingId.toString()) } returns null

            scheduler.markAsFailedIfExhausted(processing, RuntimeException("복구 실패"))

            verify { processing.markFailed("RECOVERY_FAILED", any()) }
            verify { processingCommand.update(processing) }
        }

        @Test
        @DisplayName("요청자 memberId가 있으면 SSE 실패 알림을 전송한다")
        fun shouldSendSseWhenMemberIdExists() {
            val processingId = UUID.randomUUID()
            val processing = mockk<JdSummaryProcessing>(relaxed = true)
            every { processing.id } returns processingId

            every {
                jobSummaryRequestWriteService.failRequest(processingId.toString())
            } returns 999L

            scheduler.markAsFailedIfExhausted(processing, RuntimeException("오류"))

            verify {
                sseEmitterManager.send(
                    memberId = 999L,
                    eventName = "JOB_SUMMARY_FAILED",
                    data = match<Map<String, Any>> { payload ->
                        payload["requestId"] == processingId.toString() &&
                                payload["errorCode"] == "RECOVERY_FAILED" &&
                                payload["retryable"] == false
                    }
                )
            }
        }

        @Test
        @DisplayName("요청자 memberId가 null이면 SSE 알림을 전송하지 않는다")
        fun shouldNotSendSseWhenNoMemberId() {
            val processingId = UUID.randomUUID()
            val processing = mockk<JdSummaryProcessing>(relaxed = true)
            every { processing.id } returns processingId

            every { jobSummaryRequestWriteService.failRequest(processingId.toString()) } returns null

            scheduler.markAsFailedIfExhausted(processing, RuntimeException("오류"))

            verify(exactly = 0) { sseEmitterManager.send(any(), any(), any()) }
        }
    }
}
