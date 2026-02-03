package com.hirelog.api.job.application.summary

import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brandposition.application.BrandPositionWriteService
import com.hirelog.api.common.application.outbox.OutboxEventWriteService
import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.company.application.CompanyCandidateWriteService
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.intake.model.IntakeHashes
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.snapshot.JobSnapshotWriteService
import com.hirelog.api.job.application.snapshot.port.JobSnapshotQuery
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.pipeline.JdSummaryGenerationFacade
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryInsightResult
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.CareerType
import com.hirelog.api.job.domain.JdSummaryProcessing
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.JobSummary
import com.hirelog.api.job.domain.RecruitmentPeriodType
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.view.PositionSummaryView
import com.hirelog.api.position.domain.PositionStatus
import com.hirelog.api.relation.application.jobsummary.MemberJobSummaryWriteService
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@ExtendWith(MockKExtension::class)
class JdSummaryGenerationFacadeTest {

    @MockK lateinit var processingWriteService: JdSummaryProcessingWriteService
    @MockK lateinit var snapshotWriteService: JobSnapshotWriteService
    @MockK lateinit var snapshotQuery: JobSnapshotQuery
    @MockK lateinit var jdIntakePolicy: JdIntakePolicy
    @MockK lateinit var llmClient: JobSummaryLlm
    @MockK lateinit var summaryWriteService: JobSummaryWriteService
    @MockK lateinit var brandWriteService: BrandWriteService
    @MockK lateinit var brandPositionWriteService: BrandPositionWriteService
    @MockK lateinit var positionQuery: PositionQuery
    @MockK lateinit var outboxEventWriteService: OutboxEventWriteService
    @MockK lateinit var memberJobSummaryWriteService: MemberJobSummaryWriteService
    @MockK lateinit var companyCandidateWriteService: CompanyCandidateWriteService
    @MockK lateinit var companyQuery: CompanyQuery

    private lateinit var pipeline: JdSummaryGenerationFacade

    @BeforeEach
    fun setUp() {
        pipeline = JdSummaryGenerationFacade(
            processingWriteService = processingWriteService,
            snapshotWriteService = snapshotWriteService,
            snapshotQuery = snapshotQuery,
            jdIntakePolicy = jdIntakePolicy,
            llmClient = llmClient,
            summaryWriteService = summaryWriteService,
            brandWriteService = brandWriteService,
            brandPositionWriteService = brandPositionWriteService,
            positionQuery = positionQuery,
            outboxEventWriteService = outboxEventWriteService,
            memberJobSummaryWriteService = memberJobSummaryWriteService,
            companyCandidateWriteService = companyCandidateWriteService,
            companyQuery = companyQuery
        )
    }

    /* =========================
     * Test Fixtures
     * ========================= */

    private fun createCommand(
        requestId: String = "req-${UUID.randomUUID()}"
    ) = JobSummaryGenerateCommand(
        requestId = requestId,
        brandName = "테스트회사",
        positionName = "백엔드 개발자",
        source = JobSourceType.TEXT,
        sourceUrl = "https://example.com",
        canonicalMap = mapOf(
            "responsibilities" to listOf("API 개발"),
            "requirements" to listOf("Java"),
            "preferred" to listOf("Spring")
        ),
        recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
        openedDate = null,
        closedDate = null,
        skills = emptyList(),
        occurredAt = System.currentTimeMillis()
    )

    private fun createProcessing(id: UUID = UUID.randomUUID()) =
        mockk<JdSummaryProcessing> {
            every { this@mockk.id } returns id
        }

    private fun createHashes() =
        IntakeHashes(
            canonicalHash = "hash",
            simHash = 123L,
            coreText = "core"
        )

    private fun createLlmResult() =
        JobSummaryLlmResult(
            llmProvider = LlmProvider.GEMINI,
            brandName = "테스트회사",
            positionName = "Backend Engineer",
            brandPositionName = "백엔드 개발자",
            companyCandidate = null,
            careerType = CareerType.EXPERIENCED,
            careerYears = "3년 이상",
            summary = "요약",
            responsibilities = "API",
            requiredQualifications = "Java",
            preferredQualifications = null,
            techStack = "Kotlin",
            recruitmentProcess = null,
            insight = JobSummaryInsightResult(
                idealCandidate = null,
                mustHaveSignals = null,
                preparationFocus = null,
                transferableStrengthsAndGapPlan = null,
                proofPointsAndMetrics = null,
                storyAngles = null,
                keyChallenges = null,
                technicalContext = null,
                questionsToAsk = null,
                considerations = null
            )
        )

    private fun createPosition(
        id: Long = 1L,
        name: String = "Backend Engineer",
        categoryId: Long = 1L,
        categoryName: String = "Engineering"
    ) = PositionSummaryView(
        id = id,
        name = name,
        status = PositionStatus.ACTIVE,
        categoryId = categoryId,
        categoryName = categoryName
    )

    private fun createBrand() =
        mockk<Brand> {
            every { id } returns 1L
        }

    /* =========================
     * Tests
     * ========================= */

    @Nested
    @DisplayName("LLM Timeout")
    inner class LlmTimeoutTest {

        @Test
        fun `LLM timeout → FAILED 처리`() {
            val command = createCommand()
            val processing = createProcessing()

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { snapshotQuery.loadSnapshotsByUrl(any()) } returns emptyList()
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns createHashes()
            every { jdIntakePolicy.decideDuplicate(any(), any()) } returns DuplicateDecision.NOT_DUPLICATE
            every { snapshotWriteService.record(any()) } returns 1L
            every { processingWriteService.markSummarizing(any()) } just Runs
            every { positionQuery.findActiveNames() } returns listOf("Backend Engineer")
            every { companyQuery.findAllNames() } returns emptyList()

            every {
                llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any())
            } returns CompletableFuture.failedFuture(TimeoutException("timeout"))

            val errorCode = slot<String>()
            every { processingWriteService.markFailed(any(), capture(errorCode), any()) } just Runs

            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            assertEquals("LLM_TIMEOUT", errorCode.captured)
        }
    }

    @Nested
    @DisplayName("Position Mapping")
    inner class PositionMappingTest {

        @Test
        fun `정상 Position 매핑`() {
            val command = createCommand()
            val processing = createProcessing()
            val llmResult = createLlmResult()
            val position = createPosition()
            val brand = createBrand()
            val summary = mockk<JobSummary> {
                every { id } returns 1L
            }

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { snapshotQuery.loadSnapshotsByUrl(any()) } returns emptyList()
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns createHashes()
            every { jdIntakePolicy.decideDuplicate(any(), any()) } returns DuplicateDecision.NOT_DUPLICATE
            every { snapshotWriteService.record(any()) } returns 1L
            every { processingWriteService.markSummarizing(any()) } just Runs
            every { positionQuery.findActiveNames() } returns listOf("Backend Engineer")
            every { companyQuery.findAllNames() } returns emptyList()

            every {
                llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any())
            } returns CompletableFuture.completedFuture(llmResult)

            every { brandWriteService.getOrCreate(any(), any(), any(), any()) } returns brand
            every { positionQuery.findByNormalizedName(any()) } returns position
            every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns mockk()
            every { summaryWriteService.save(any(), any(), any(), any(), any(), any()) } returns summary
            every { outboxEventWriteService.append(any()) } just Runs
            every { processingWriteService.markCompleted(any()) } just Runs

            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            verify {
                summaryWriteService.save(
                    any(),
                    brand,
                    position.id,
                    position.name,
                    llmResult,
                    command.positionName
                )
            }
        }

        @Test
        fun `UNKNOWN fallback`() {
            val command = createCommand()
            val processing = createProcessing()
            val llmResult = createLlmResult()
            val unknown = createPosition(id = 999, name = "UNKNOWN")
            val brand = createBrand()
            val summary = mockk<JobSummary> {
                every { id } returns 1L
            }

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { snapshotQuery.loadSnapshotsByUrl(any()) } returns emptyList()
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns createHashes()
            every { jdIntakePolicy.decideDuplicate(any(), any()) } returns DuplicateDecision.NOT_DUPLICATE
            every { snapshotWriteService.record(any()) } returns 1L
            every { processingWriteService.markSummarizing(any()) } just Runs
            every { positionQuery.findActiveNames() } returns listOf("Backend Engineer")
            every { companyQuery.findAllNames() } returns emptyList()

            every {
                llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any())
            } returns CompletableFuture.completedFuture(llmResult)

            every { brandWriteService.getOrCreate(any(), any(), any(), any()) } returns brand
            every { positionQuery.findByNormalizedName(match { it != "unknown" }) } returns null
            every { positionQuery.findByNormalizedName("unknown") } returns unknown
            every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns mockk()
            every { summaryWriteService.save(any(), any(), any(), any(), any(), any()) } returns summary
            every { outboxEventWriteService.append(any()) } just Runs
            every { processingWriteService.markCompleted(any()) } just Runs

            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            verify {
                summaryWriteService.save(any(), brand, 999L, "UNKNOWN", llmResult, command.positionName)
            }
        }
    }
}
