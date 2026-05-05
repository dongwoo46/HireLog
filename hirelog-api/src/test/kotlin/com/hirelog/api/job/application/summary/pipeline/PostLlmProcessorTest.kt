package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.brand.application.BrandWriteService
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.company.application.CompanyCandidateWriteService
import com.hirelog.api.job.application.summary.JobSummaryWriteService
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.view.JobSummaryInsightResult
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.model.JobSummary
import com.hirelog.api.job.domain.type.CareerType
import com.hirelog.api.job.domain.type.CompanyDomain
import com.hirelog.api.job.domain.type.CompanySize
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.domain.type.RecruitmentPeriodType
import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.domain.Position
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import com.hirelog.api.relation.domain.model.BrandPosition
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.util.UUID

@DisplayName("PostLlmProcessor 테스트")
class PostLlmProcessorTest {

    private lateinit var processor: PostLlmProcessor
    private lateinit var brandWriteService: BrandWriteService
    private lateinit var brandPositionWriteService: BrandPositionWriteService
    private lateinit var positionCommand: PositionCommand
    private lateinit var companyCandidateWriteService: CompanyCandidateWriteService
    private lateinit var summaryWriteService: JobSummaryWriteService

    private val processingId = UUID.randomUUID()

    private val insightResult = JobSummaryInsightResult(
        idealCandidate = null, mustHaveSignals = null, preparationFocus = null,
        transferableStrengthsAndGapPlan = null, proofPointsAndMetrics = null,
        storyAngles = null, keyChallenges = null, technicalContext = null,
        questionsToAsk = null, considerations = null
    )

    private fun makeLlmResult(companyCandidate: String? = null) = JobSummaryLlmResult(
        llmProvider = LlmProvider.GEMINI,
        brandName = "Toss",
        positionName = "Backend Engineer",
        companyCandidate = companyCandidate,
        companyDomain = CompanyDomain.OTHER,
        companySize = CompanySize.UNKNOWN,
        careerType = CareerType.EXPERIENCED,
        careerYears = "3년 이상",
        summary = "요약",
        responsibilities = "업무",
        requiredQualifications = "자격",
        preferredQualifications = null,
        techStack = null,
        recruitmentProcess = null,
        insight = insightResult
    )

    @BeforeEach
    fun setUp() {
        brandWriteService = mockk()
        brandPositionWriteService = mockk()
        positionCommand = mockk()
        companyCandidateWriteService = mockk(relaxed = true)
        summaryWriteService = mockk(relaxed = true)
        processor = PostLlmProcessor(
            brandWriteService, brandPositionWriteService, positionCommand,
            companyCandidateWriteService, summaryWriteService
        )
        mockkObject(Normalizer)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(Normalizer)
    }

    @Nested
    @DisplayName("resolve 메서드는")
    inner class ResolveTest {

        @Test
        @DisplayName("Brand, Position, BrandPosition을 정상 조회하고 반환한다")
        fun shouldResolveEntitiesSuccessfully() {
            val brand = mockk<Brand>(relaxed = true)
            val position = mockk<Position>(relaxed = true)
            val brandPosition = mockk<BrandPosition>(relaxed = true)

            every { Normalizer.normalizePosition("Backend Engineer") } returns "backend_engineer"
            every { brandWriteService.getOrCreate(any(), any(), any()) } returns brand
            every { positionCommand.findByNormalizedName("backend_engineer") } returns position
            every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns brandPosition

            val result = processor.resolve(makeLlmResult(), "Toss Backend")

            assertThat(result.brand).isEqualTo(brand)
            assertThat(result.position).isEqualTo(position)
            assertThat(result.brandPosition).isEqualTo(brandPosition)
        }

        @Test
        @DisplayName("Position을 찾을 수 없으면 예외를 던진다")
        fun shouldThrowWhenPositionNotFound() {
            val brand = mockk<Brand>(relaxed = true)

            every { Normalizer.normalizePosition("Backend Engineer") } returns "backend_engineer"
            every { brandWriteService.getOrCreate(any(), any(), any()) } returns brand
            every { positionCommand.findByNormalizedName("backend_engineer") } returns null

            assertThatThrownBy {
                processor.resolve(makeLlmResult(), "Toss Backend")
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("UNKNOWN position not found")
        }
    }

    @Nested
    @DisplayName("execute 메서드는")
    inner class ExecuteTest {

        private val command = JobSummaryGenerateCommand(
            requestId = processingId.toString(),
            brandName = "Toss",
            positionName = "Backend Engineer",
            source = JobSourceType.TEXT,
            sourceUrl = null,
            canonicalMap = emptyMap(),
            recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
            openedDate = null,
            closedDate = null,
            skills = emptyList(),
            occurredAt = System.currentTimeMillis()
        )

        @Test
        @DisplayName("정상 흐름에서 createWithOutbox를 호출한다")
        fun shouldCallCreateWithOutbox() {
            val brand = mockk<Brand>(relaxed = true)
            val position = mockk<Position>(relaxed = true)
            val brandPosition = mockk<BrandPosition>(relaxed = true)
            val savedSummary = mockk<JobSummary>(relaxed = true)

            every { Normalizer.normalizePosition(any()) } returns "backend_engineer"
            every { brandWriteService.getOrCreate(any(), any(), any()) } returns brand
            every { positionCommand.findByNormalizedName(any()) } returns position
            every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns brandPosition
            every { summaryWriteService.createWithOutbox(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns savedSummary

            processor.execute(
                snapshotId = 10L,
                llmResult = makeLlmResult(companyCandidate = null),
                processingId = processingId,
                command = command
            )

            verify { summaryWriteService.createWithOutbox(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            verify(exactly = 0) { companyCandidateWriteService.createCandidate(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("companyCandidate가 있으면 CompanyCandidate를 생성한다")
        fun shouldCreateCompanyCandidateWhenPresent() {
            val brand = mockk<Brand>(relaxed = true)
            val position = mockk<Position>(relaxed = true)
            val brandPosition = mockk<BrandPosition>(relaxed = true)
            val savedSummary = mockk<JobSummary>(relaxed = true)

            every { Normalizer.normalizePosition(any()) } returns "backend_engineer"
            every { brandWriteService.getOrCreate(any(), any(), any()) } returns brand
            every { positionCommand.findByNormalizedName(any()) } returns position
            every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns brandPosition
            every { summaryWriteService.createWithOutbox(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns savedSummary

            processor.execute(
                snapshotId = 10L,
                llmResult = makeLlmResult(companyCandidate = "(주)비바리퍼블리카"),
                processingId = processingId,
                command = command
            )

            verify { companyCandidateWriteService.createCandidate(any(), any(), eq("(주)비바리퍼블리카"), any(), any()) }
        }

        @Test
        @DisplayName("CompanyCandidate 생성 실패는 전체 흐름에 영향을 주지 않는다")
        fun shouldNotPropagateCompanyCandidateError() {
            val brand = mockk<Brand>(relaxed = true)
            val position = mockk<Position>(relaxed = true)
            val brandPosition = mockk<BrandPosition>(relaxed = true)
            val savedSummary = mockk<JobSummary>(relaxed = true)

            every { Normalizer.normalizePosition(any()) } returns "backend_engineer"
            every { brandWriteService.getOrCreate(any(), any(), any()) } returns brand
            every { positionCommand.findByNormalizedName(any()) } returns position
            every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns brandPosition
            every { summaryWriteService.createWithOutbox(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns savedSummary
            every { companyCandidateWriteService.createCandidate(any(), any(), any(), any(), any()) } throws RuntimeException("duplicate")

            // 예외 없이 완료되어야 함
            processor.execute(
                snapshotId = 10L,
                llmResult = makeLlmResult(companyCandidate = "비바리퍼블리카"),
                processingId = processingId,
                command = command
            )
        }
    }
}
