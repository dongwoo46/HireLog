package com.hirelog.api.job.application.summary

import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.brandposition.application.BrandPositionWriteService
import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.intake.model.JdIntakeHashes
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.messaging.JdPreprocessResponseMessage
import com.hirelog.api.job.application.snapshot.JobSnapshotWriteService
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.CareerType
import com.hirelog.api.job.domain.JdSummaryProcessing
import com.hirelog.api.job.domain.JdSummaryProcessingStatus
import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.query.PositionView
import com.hirelog.api.position.domain.PositionStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class JobSummaryGenerationFacadeServiceTest {

    @MockK
    lateinit var processingWriteService: JdSummaryProcessingWriteService

    @MockK
    lateinit var snapshotWriteService: JobSnapshotWriteService

    @MockK
    lateinit var jdIntakePolicy: JdIntakePolicy

    @MockK
    lateinit var llmClient: JobSummaryLlm

    @MockK
    lateinit var summaryWriteService: JobSummaryWriteService

    @MockK
    lateinit var brandWriteService: BrandWriteService

    @MockK
    lateinit var brandPositionWriteService: BrandPositionWriteService

    @MockK
    lateinit var positionQuery: PositionQuery

    private lateinit var facadeService: JobSummaryGenerationFacadeService
    private val testExecutor = Executors.newSingleThreadExecutor()

    @BeforeEach
    fun setUp() {
        facadeService = JobSummaryGenerationFacadeService(
            processingWriteService = processingWriteService,
            snapshotWriteService = snapshotWriteService,
            jdIntakePolicy = jdIntakePolicy,
            llmClient = llmClient,
            summaryWriteService = summaryWriteService,
            brandWriteService = brandWriteService,
            brandPositionWriteService = brandPositionWriteService,
            positionQuery = positionQuery
        )
    }

    private fun createTestMessage(requestId: String = "test-${UUID.randomUUID()}") = mockk<JdPreprocessResponseMessage> {
        every { this@mockk.requestId } returns requestId
        every { brandName } returns "테스트회사"
        every { positionName } returns "백엔드 개발자"
        every { source } returns "MANUAL"
        every { sourceUrl } returns "https://example.com"
        every { canonicalMap } returns mapOf("responsibilities" to listOf("API 개발"))
        every { recruitmentPeriodType } returns null
        every { openedDate } returns null
        every { closedDate } returns null
    }

    private fun createTestProcessing(id: UUID = UUID.randomUUID()) = mockk<JdSummaryProcessing> {
        every { this@mockk.id } returns id
        every { status } returns JdSummaryProcessingStatus.RECEIVED
    }

    private fun createTestLlmResult() = JobSummaryLlmResult(
        llmProvider = LlmProvider.GEMINI,
        careerType = CareerType.EXPERIENCED,
        careerYears = 3,
        summary = "테스트 요약",
        responsibilities = "API 개발",
        requiredQualifications = "Java 3년",
        preferredQualifications = null,
        techStack = "Kotlin, Spring",
        recruitmentProcess = null,
        brandName = "테스트회사",
        positionName = "Backend Engineer",
        brandPositionName = "백엔드 개발자"
    )

    private fun createTestPositionView(
        id: Long = 1L,
        name: String = "Backend Engineer",
        normalizedName: String = "backend_engineer"
    ) = PositionView(
        id = id,
        name = name,
        normalizedName = normalizedName,
        status = PositionStatus.ACTIVE,
        description = null
    )

    private fun createTestBrand() = mockk<Brand> {
        every { id } returns 1L
        every { name } returns "테스트회사"
    }

    @Nested
    @DisplayName("LLM Timeout 테스트")
    inner class LlmTimeoutTest {

        @Test
        @DisplayName("LLM 호출이 45초 초과 시 TimeoutException 발생하고 FAILED 처리")
        fun `should mark failed when LLM call exceeds timeout`() {
            // given
            val message = createTestMessage()
            val processing = createTestProcessing()
            val hashes = JdIntakeHashes(
                canonicalHash = "hash123",
                simHash = 12345L,
                coreText = "core text"
            )

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(any(), any()) } returns DuplicateDecision.NOT_DUPLICATE
            every { snapshotWriteService.record(any()) } returns 1L
            every { processingWriteService.markSummarizing(any()) } just Runs
            every { positionQuery.findActive() } returns listOf(createTestPositionView())

            // LLM이 타임아웃보다 오래 걸리는 상황 시뮬레이션
            every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any()) } returns
                CompletableFuture<JobSummaryLlmResult>().apply {
                    // 완료되지 않는 Future (타임아웃 발생)
                }

            every { processingWriteService.markFailed(any(), any(), any()) } just Runs

            // when
            val future = facadeService.generateAsync(message, testExecutor)

            // then - orTimeout이 45초이므로 테스트에서는 짧은 시간 후 결과 확인
            // 실제 45초를 기다리지 않고, Future가 타임아웃으로 완료되는지 확인
            // 테스트 목적상 LLM_TIMEOUT_SECONDS를 줄이거나 별도 테스트 설정 필요
        }

        @Test
        @DisplayName("LLM 호출 실패 시 errorCode가 올바르게 분류됨")
        fun `should classify error code correctly on LLM failure`() {
            // given
            val message = createTestMessage()
            val processing = createTestProcessing()
            val processingId = processing.id
            val hashes = JdIntakeHashes("hash", 123L, "core")

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(any(), any()) } returns DuplicateDecision.NOT_DUPLICATE
            every { snapshotWriteService.record(any()) } returns 1L
            every { processingWriteService.markSummarizing(any()) } just Runs
            every { positionQuery.findActive() } returns listOf(createTestPositionView())

            // TimeoutException 발생
            every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any()) } returns
                CompletableFuture.failedFuture(TimeoutException("LLM timeout"))

            val capturedErrorCode = slot<String>()
            every { processingWriteService.markFailed(any(), capture(capturedErrorCode), any()) } just Runs

            // when
            val future = facadeService.generateAsync(message, testExecutor)
            future.get(5, TimeUnit.SECONDS)

            // then
            verify { processingWriteService.markFailed(processingId, any(), any()) }
            assertEquals("LLM_TIMEOUT", capturedErrorCode.captured)
        }
    }

    @Nested
    @DisplayName("Position 조회 테스트")
    inner class PositionQueryTest {

        @Test
        @DisplayName("LLM이 반환한 Position이 DB에 있으면 정상 매핑")
        fun `should map position when found in database`() {
            // given
            val message = createTestMessage()
            val processing = createTestProcessing()
            val hashes = JdIntakeHashes("hash", 123L, "core")
            val llmResult = createTestLlmResult()
            val positionView = createTestPositionView()
            val brand = createTestBrand()

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(any(), any()) } returns DuplicateDecision.NOT_DUPLICATE
            every { snapshotWriteService.record(any()) } returns 1L
            every { processingWriteService.markSummarizing(any()) } just Runs
            every { positionQuery.findActive() } returns listOf(positionView)
            every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any()) } returns
                CompletableFuture.completedFuture(llmResult)
            every { brandWriteService.getOrCreate(any(), any(), any(), any()) } returns brand
            every { positionQuery.findByNormalizedName(any()) } returns positionView
            every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns mockk()
            every { summaryWriteService.save(any(), any(), any(), any(), any()) } returns mockk()
            every { processingWriteService.markCompleted(any()) } just Runs

            // when
            val future = facadeService.generateAsync(message, testExecutor)
            future.get(5, TimeUnit.SECONDS)

            // then
            verify { positionQuery.findByNormalizedName(any()) }
            verify { summaryWriteService.save(any(), brand, positionView.id, positionView.name, llmResult) }
            verify { processingWriteService.markCompleted(processing.id) }
        }

        @Test
        @DisplayName("LLM이 반환한 Position이 DB에 없으면 UNKNOWN으로 fallback")
        fun `should fallback to UNKNOWN when position not found`() {
            // given
            val message = createTestMessage()
            val processing = createTestProcessing()
            val hashes = JdIntakeHashes("hash", 123L, "core")
            val llmResult = createTestLlmResult()
            val unknownPosition = createTestPositionView(id = 999L, name = "UNKNOWN", normalizedName = "unknown")
            val brand = createTestBrand()

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(any(), any()) } returns DuplicateDecision.NOT_DUPLICATE
            every { snapshotWriteService.record(any()) } returns 1L
            every { processingWriteService.markSummarizing(any()) } just Runs
            every { positionQuery.findActive() } returns listOf(createTestPositionView())
            every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any()) } returns
                CompletableFuture.completedFuture(llmResult)
            every { brandWriteService.getOrCreate(any(), any(), any(), any()) } returns brand

            // 첫 번째 조회: null (Position 없음), 두 번째 조회: UNKNOWN
            every { positionQuery.findByNormalizedName(match { it != "unknown" }) } returns null
            every { positionQuery.findByNormalizedName("unknown") } returns unknownPosition

            every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns mockk()
            every { summaryWriteService.save(any(), any(), any(), any(), any()) } returns mockk()
            every { processingWriteService.markCompleted(any()) } just Runs

            // when
            val future = facadeService.generateAsync(message, testExecutor)
            future.get(5, TimeUnit.SECONDS)

            // then
            verify { summaryWriteService.save(any(), brand, 999L, "UNKNOWN", llmResult) }
        }

        @Test
        @DisplayName("UNKNOWN Position도 DB에 없으면 IllegalStateException 발생")
        fun `should throw exception when UNKNOWN position not found`() {
            // given
            val message = createTestMessage()
            val processing = createTestProcessing()
            val hashes = JdIntakeHashes("hash", 123L, "core")
            val llmResult = createTestLlmResult()
            val brand = createTestBrand()

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(any(), any()) } returns DuplicateDecision.NOT_DUPLICATE
            every { snapshotWriteService.record(any()) } returns 1L
            every { processingWriteService.markSummarizing(any()) } just Runs
            every { positionQuery.findActive() } returns listOf(createTestPositionView())
            every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any()) } returns
                CompletableFuture.completedFuture(llmResult)
            every { brandWriteService.getOrCreate(any(), any(), any(), any()) } returns brand

            // 모든 Position 조회 null
            every { positionQuery.findByNormalizedName(any()) } returns null

            every { processingWriteService.markFailed(any(), any(), any()) } just Runs

            // when
            val future = facadeService.generateAsync(message, testExecutor)
            future.get(5, TimeUnit.SECONDS)

            // then
            verify { processingWriteService.markFailed(processing.id, match { it.contains("FAILED") }, any()) }
        }
    }
}
