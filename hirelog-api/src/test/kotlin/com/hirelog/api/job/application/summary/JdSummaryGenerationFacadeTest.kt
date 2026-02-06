package com.hirelog.api.job.application.summary

import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import com.hirelog.api.company.application.CompanyCandidateWriteService
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.common.application.outbox.OutboxEventWriteService
import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.intake.model.DuplicateReason
import com.hirelog.api.job.application.intake.model.IntakeHashes
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.snapshot.JobSnapshotWriteService
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.pipeline.JdSummaryGenerationFacade
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.view.JobSummaryInsightResult
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.*
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.view.PositionSummaryView
import com.hirelog.api.position.domain.PositionStatus
import com.hirelog.api.relation.domain.model.BrandPosition
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
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
@DisplayName("JD 요약 생성 파이프라인")
class JdSummaryGenerationFacadeTest {

    @MockK lateinit var processingWriteService: JdSummaryProcessingWriteService
    @MockK lateinit var snapshotWriteService: JobSnapshotWriteService
    @MockK lateinit var jdIntakePolicy: JdIntakePolicy
    @MockK lateinit var llmClient: JobSummaryLlm
    @MockK lateinit var summaryWriteService: JobSummaryWriteService
    @MockK lateinit var summaryQuery: JobSummaryQuery
    @MockK lateinit var brandWriteService: BrandWriteService
    @MockK lateinit var brandPositionWriteService: BrandPositionWriteService
    @MockK lateinit var positionQuery: PositionQuery
    @MockK lateinit var outboxEventWriteService: OutboxEventWriteService
    @MockK lateinit var companyCandidateWriteService: CompanyCandidateWriteService
    @MockK lateinit var companyQuery: CompanyQuery

    private lateinit var pipeline: JdSummaryGenerationFacade

    @BeforeEach
    fun setUp() {
        pipeline = JdSummaryGenerationFacade(
            processingWriteService = processingWriteService,
            snapshotWriteService = snapshotWriteService,
            jdIntakePolicy = jdIntakePolicy,
            llmClient = llmClient,
            summaryWriteService = summaryWriteService,
            summaryQuery = summaryQuery,
            brandWriteService = brandWriteService,
            brandPositionWriteService = brandPositionWriteService,
            positionQuery = positionQuery,
            outboxEventWriteService = outboxEventWriteService,
            companyCandidateWriteService = companyCandidateWriteService,
            companyQuery = companyQuery
        )
    }

    /* =========================
     * Test Fixtures
     * ========================= */

    private fun createValidCommand(
        requestId: String = "req-${UUID.randomUUID()}",
        brandName: String = "테스트회사",
        positionName: String = "백엔드 개발자",
        source: JobSourceType = JobSourceType.TEXT,
        sourceUrl: String? = null
    ) = JobSummaryGenerateCommand(
        requestId = requestId,
        brandName = brandName,
        positionName = positionName,
        source = source,
        sourceUrl = sourceUrl,
        canonicalMap = mapOf(
            "responsibilities" to listOf("API 개발", "데이터베이스 설계"),
            "requirements" to listOf("Java 3년 이상", "Spring Boot"),
            "preferred" to listOf("Kotlin", "AWS")
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
            canonicalHash = "hash123",
            simHash = 123456789L,
            coreText = "API 개발 데이터베이스 설계 Java Spring"
        )

    private fun createSuccessfulLlmResult(
        brandName: String = "테스트회사",
        positionName: String = "Backend Engineer",
        companyCandidate: String? = null
    ) = JobSummaryLlmResult(
        llmProvider = LlmProvider.GEMINI,
        brandName = brandName,
        positionName = positionName,
        brandPositionName = "백엔드 개발자",
        companyCandidate = companyCandidate,
        careerType = CareerType.EXPERIENCED,
        careerYears = "3년 이상",
        summary = "백엔드 API 개발 및 시스템 설계 담당",
        responsibilities = "RESTful API 개발, 데이터베이스 설계",
        requiredQualifications = "Java, Spring Boot 3년 이상",
        preferredQualifications = "Kotlin, AWS 경험자 우대",
        techStack = "Java, Spring Boot, Kotlin, AWS",
        recruitmentProcess = null,
        insight = JobSummaryInsightResult(
            idealCandidate = "백엔드 개발 3년차 이상",
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

    private fun createBrand(id: Long = 1L) =
        mockk<Brand> {
            every { this@mockk.id } returns id
        }

    private fun createJobSummary(id: Long = 1L) =
        mockk<JobSummary> {
            every { this@mockk.id } returns id
        }

    private fun createBrandPosition(id: Long = 1L) =
        mockk<BrandPosition> {
            every { this@mockk.id } returns id
        }

    /**
     * 기본 성공 경로를 위한 공통 Mock 설정
     */
    private fun setupSuccessfulPath(
        command: JobSummaryGenerateCommand = createValidCommand(),
        llmResult: JobSummaryLlmResult = createSuccessfulLlmResult(),
        position: PositionSummaryView = createPosition()
    ) {
        val processing = createProcessing()
        val brand = createBrand()
        val summary = createJobSummary()
        val brandPosition = createBrandPosition()

        every { processingWriteService.startProcessing(any()) } returns processing
        every { jdIntakePolicy.isValidJd(any()) } returns true
        every { summaryQuery.existsBySourceUrl(any()) } returns false
        every { jdIntakePolicy.generateIntakeHashes(any()) } returns createHashes()
        every { jdIntakePolicy.decideDuplicate(any(), any()) } returns DuplicateDecision.NotDuplicate
        every { snapshotWriteService.record(any()) } returns 1L
        every { processingWriteService.markSummarizing(any()) } just Runs
        every { positionQuery.findActiveNames() } returns listOf("Backend Engineer", "Frontend Engineer")
        every { companyQuery.findAllNames() } returns emptyList()
        every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) } returns
                CompletableFuture.completedFuture(llmResult)
        every { brandWriteService.getOrCreate(any(), any(), any(), any()) } returns brand
        every { positionQuery.findByNormalizedName(any()) } returns position
        every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns brandPosition

        // JobSummaryWriteService.save() - 모든 파라미터 포함
        every {
            summaryWriteService.save(
                snapshotId = any(),
                brand = any(),
                positionId = any(),
                positionName = any(),
                brandPositionId = any(),
                positionCategoryId = any(),
                positionCategoryName = any(),
                llmResult = any(),
                brandPositionName = any(),
                sourceUrl = any()
            )
        } returns summary

        every { outboxEventWriteService.append(any()) } just Runs
        every { processingWriteService.markCompleted(any()) } just Runs

        // companyCandidateWriteService - CompanyCandidate 반환
        every {
            companyCandidateWriteService.createCandidate(
                jdSummaryId = any(),
                brandId = any(),
                candidateName = any(),
                source = any(),
                confidenceScore = any()
            )
        } returns mockk()  // CompanyCandidate mock 객체 반환
    }

    /* =========================
     * 핵심 성공 시나리오
     * ========================= */

    @Nested
    @DisplayName("정상 처리 시나리오")
    inner class SuccessScenarios {

        @Test
        fun `정상 JD 입력 시 요약 생성 완료`() {
            // Given
            val command = createValidCommand()
            setupSuccessfulPath(command = command)

            // When
            val result = pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            assertNull(result) // CompletableFuture<Void>
            verify(exactly = 1) { processingWriteService.markCompleted(any()) }
            verify(exactly = 1) {
                summaryWriteService.save(
                    snapshotId = any(),
                    brand = any(),
                    positionId = any(),
                    positionName = any(),
                    brandPositionId = any(),
                    positionCategoryId = any(),
                    positionCategoryName = any(),
                    llmResult = any(),
                    brandPositionName = any(),
                    sourceUrl = any()
                )
            }
            verify(exactly = 1) { outboxEventWriteService.append(any()) }
        }

        @Test
        fun `LLM이 올바른 Position을 선택하면 해당 Position으로 저장`() {
            // Given
            val command = createValidCommand(positionName = "백엔드 개발자")
            val llmResult = createSuccessfulLlmResult(positionName = "Backend Engineer")
            val backendPosition = createPosition(id = 10, name = "Backend Engineer", categoryId = 2, categoryName = "Development")

            setupSuccessfulPath(command = command, llmResult = llmResult, position = backendPosition)

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify {
                summaryWriteService.save(
                    snapshotId = any(),
                    brand = any(),
                    positionId = 10L,
                    positionName = "Backend Engineer",
                    brandPositionId = any(),
                    positionCategoryId = 2L,
                    positionCategoryName = "Development",
                    llmResult = llmResult,
                    brandPositionName = "백엔드 개발자",
                    sourceUrl = any()
                )
            }
        }

        @Test
        fun `CompanyCandidate가 있으면 생성 시도`() {
            // Given
            val command = createValidCommand(brandName = "스타트업A")
            val llmResult = createSuccessfulLlmResult(
                brandName = "스타트업A",
                companyCandidate = "주식회사 스타트업A"
            )

            setupSuccessfulPath(command = command, llmResult = llmResult)

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify {
                companyCandidateWriteService.createCandidate(
                    jdSummaryId = any(),
                    brandId = any(),
                    candidateName = "주식회사 스타트업A",
                    source = any(),
                    confidenceScore = 0.7
                )
            }
        }
    }

    /* =========================
     * Pre-LLM 검증 실패
     * ========================= */

    @Nested
    @DisplayName("Pre-LLM 검증 단계")
    inner class PreLlmValidation {

        @Test
        fun `유효하지 않은 JD 입력 시 INVALID_INPUT 처리`() {
            // Given
            val command = createValidCommand()
            val processing = createProcessing()

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(command) } returns false
            every { processingWriteService.markFailed(any(), any(), any()) } just Runs

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify {
                processingWriteService.markFailed(
                    processingId = processing.id,
                    errorCode = "INVALID_INPUT",
                    errorMessage = any()
                )
            }
            verify(exactly = 0) { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) }
        }

        @Test
        fun `URL 중복 시 처리 중단`() {
            // Given
            val command = createValidCommand(
                source = JobSourceType.URL,
                sourceUrl = "https://example.com/job/123"
            )
            val processing = createProcessing()

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { summaryQuery.existsBySourceUrl("https://example.com/job/123") } returns true
            every { processingWriteService.markDuplicate(any(), any()) } just Runs

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify {
                processingWriteService.markDuplicate(
                    processingId = processing.id,
                    reason = "URL_DUPLICATE"
                )
            }
            verify(exactly = 0) { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) }
        }

        /**
         * 중복 감지 공통 테스트 헬퍼
         */
        private fun testDuplicateDetection(
            reason: DuplicateReason,
            existingSnapshotId: Long,
            existingSummaryId: Long?
        ) {
            // Given
            val command = createValidCommand()
            val processing = createProcessing()
            val hashes = createHashes()
            val duplicateDecision = DuplicateDecision.Duplicate(
                reason = reason,
                existingSnapshotId = existingSnapshotId,
                existingSummaryId = existingSummaryId
            )

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { summaryQuery.existsBySourceUrl(any()) } returns false
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(command, hashes) } returns duplicateDecision
            every { processingWriteService.markDuplicate(any(), any()) } just Runs

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify {
                processingWriteService.markDuplicate(
                    processingId = processing.id,
                    reason = reason.name
                )
            }
            verify(exactly = 0) { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) }
        }

        @Test
        fun `HASH 중복 감지 시 DUPLICATE 처리`() {
            testDuplicateDetection(
                reason = DuplicateReason.HASH,
                existingSnapshotId = 999L,
                existingSummaryId = 888L
            )
        }

        @Test
        fun `SIMHASH 중복 감지 시 DUPLICATE 처리`() {
            testDuplicateDetection(
                reason = DuplicateReason.SIMHASH,
                existingSnapshotId = 777L,
                existingSummaryId = null  // Summary가 없는 경우
            )
        }

        @Test
        fun `TRGM 중복 감지 시 DUPLICATE 처리`() {
            testDuplicateDetection(
                reason = DuplicateReason.TRGM,
                existingSnapshotId = 666L,
                existingSummaryId = 555L
            )
        }

        @Test
        fun `URL 중복 감지 시 DUPLICATE 처리`() {
            testDuplicateDetection(
                reason = DuplicateReason.URL,
                existingSnapshotId = 444L,
                existingSummaryId = 333L
            )
        }

        @Test
        fun `중복이 아니면 정상 처리 진행`() {
            // Given
            val command = createValidCommand()
            val processing = createProcessing()
            val hashes = createHashes()

            every { processingWriteService.startProcessing(any()) } returns processing
            every { jdIntakePolicy.isValidJd(any()) } returns true
            every { summaryQuery.existsBySourceUrl(any()) } returns false
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(command, hashes) } returns DuplicateDecision.NotDuplicate
            every { snapshotWriteService.record(any()) } returns 1L
            every { processingWriteService.markSummarizing(any()) } just Runs
            every { positionQuery.findActiveNames() } returns listOf("Backend Engineer")
            every { companyQuery.findAllNames() } returns emptyList()

            // LLM 호출이 진행되는지 확인
            every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) } returns
                    CompletableFuture.completedFuture(createSuccessfulLlmResult())

            val brand = createBrand()
            val position = createPosition()
            val brandPosition = createBrandPosition()
            val summary = createJobSummary()

            every { brandWriteService.getOrCreate(any(), any(), any(), any()) } returns brand
            every { positionQuery.findByNormalizedName(any()) } returns position
            every { brandPositionWriteService.getOrCreate(any(), any(), any(), any()) } returns brandPosition
            every {
                summaryWriteService.save(
                    snapshotId = any(),
                    brand = any(),
                    positionId = any(),
                    positionName = any(),
                    brandPositionId = any(),
                    positionCategoryId = any(),
                    positionCategoryName = any(),
                    llmResult = any(),
                    brandPositionName = any(),
                    sourceUrl = any()
                )
            } returns summary
            every { outboxEventWriteService.append(any()) } just Runs
            every { processingWriteService.markCompleted(any()) } just Runs

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify(exactly = 0) { processingWriteService.markDuplicate(any(), any()) }
            verify(exactly = 1) { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) }
            verify(exactly = 1) { processingWriteService.markCompleted(any()) }
        }
    }

    /* =========================
     * LLM 호출 실패
     * ========================= */

    @Nested
    @DisplayName("LLM 호출 오류 처리")
    inner class LlmFailureHandling {

        /**
         * 실패한 CompletableFuture 생성 헬퍼
         */
        private fun <T> failedFuture(exception: Throwable): CompletableFuture<T> {
            return CompletableFuture<T>().apply {
                completeExceptionally(exception)
            }
        }

        @Test
        fun `LLM Timeout 발생 시 LLM_TIMEOUT 에러 처리`() {
            // Given
            val command = createValidCommand()
            setupSuccessfulPath(command = command)

            every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) } returns
                    failedFuture(TimeoutException("LLM timeout after 45s"))

            val errorCodeSlot = slot<String>()
            every { processingWriteService.markFailed(any(), capture(errorCodeSlot), any()) } just Runs

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            assertEquals("LLM_TIMEOUT", errorCodeSlot.captured)
            verify(exactly = 0) { processingWriteService.markCompleted(any()) }
        }

        @Test
        fun `LLM 호출 실패 시 LLM_CALL_FAILED 에러 처리`() {
            // Given
            val command = createValidCommand()
            setupSuccessfulPath(command = command)

            // GeminiCallException은 cause를 받으므로 실제 원인 예외 전달
            val networkError = java.net.ConnectException("Connection refused")
            every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) } returns
                    failedFuture(GeminiCallException(networkError))

            val errorCodeSlot = slot<String>()
            every { processingWriteService.markFailed(any(), capture(errorCodeSlot), any()) } just Runs

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            assertEquals("LLM_CALL_FAILED", errorCodeSlot.captured)
        }

        @Test
        fun `LLM 응답 파싱 실패 시 LLM_PARSE_FAILED 에러 처리`() {
            // Given
            val command = createValidCommand()
            setupSuccessfulPath(command = command)

            // GeminiParseException도 동일하게 cause 전달 (구조 확인 필요)
            val parseError = com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON")
            every { llmClient.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) } returns
                    failedFuture(GeminiParseException(parseError))

            val errorCodeSlot = slot<String>()
            every { processingWriteService.markFailed(any(), capture(errorCodeSlot), any()) } just Runs

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            assertEquals("LLM_PARSE_FAILED", errorCodeSlot.captured)
        }
    }

    /* =========================
     * Position 매핑
     * ========================= */

    @Nested
    @DisplayName("Position 매핑 로직")
    inner class PositionMapping {

        @Test
        fun `LLM이 선택한 Position이 존재하면 해당 Position 사용`() {
            // Given
            val command = createValidCommand()
            val llmResult = createSuccessfulLlmResult(positionName = "Backend Engineer")
            val backendPosition = createPosition(id = 5, name = "Backend Engineer", categoryId = 3, categoryName = "Tech")

            setupSuccessfulPath(command = command, llmResult = llmResult, position = backendPosition)

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify {
                summaryWriteService.save(
                    snapshotId = any(),
                    brand = any(),
                    positionId = 5L,
                    positionName = "Backend Engineer",
                    brandPositionId = any(),
                    positionCategoryId = 3L,
                    positionCategoryName = "Tech",
                    llmResult = any(),
                    brandPositionName = any(),
                    sourceUrl = any()
                )
            }
        }

        @Test
        fun `LLM이 선택한 Position이 없으면 UNKNOWN fallback`() {
            // Given
            val command = createValidCommand()
            val llmResult = createSuccessfulLlmResult(positionName = "NonExistentPosition")
            val unknownPosition = createPosition(id = 999, name = "UNKNOWN", categoryId = 0, categoryName = "Unknown")

            setupSuccessfulPath(command = command, llmResult = llmResult)

            // 첫 번째 조회는 실패, unknown 조회는 성공
            every { positionQuery.findByNormalizedName(match { it != "unknown" }) } returns null
            every { positionQuery.findByNormalizedName("unknown") } returns unknownPosition

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify {
                summaryWriteService.save(
                    snapshotId = any(),
                    brand = any(),
                    positionId = 999L,
                    positionName = "UNKNOWN",
                    brandPositionId = any(),
                    positionCategoryId = 0L,
                    positionCategoryName = "Unknown",
                    llmResult = any(),
                    brandPositionName = any(),
                    sourceUrl = any()
                )
            }
        }

        @Test
        fun `UNKNOWN Position도 없으면 에러 발생`() {
            // Given
            val command = createValidCommand()
            val llmResult = createSuccessfulLlmResult(positionName = "SomePosition")

            setupSuccessfulPath(command = command, llmResult = llmResult)

            // 모든 Position 조회 실패
            every { positionQuery.findByNormalizedName(any()) } returns null

            val errorCodeSlot = slot<String>()
            every { processingWriteService.markFailed(any(), capture(errorCodeSlot), any()) } just Runs

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            assertTrue(errorCodeSlot.captured.contains("FAILED_AT"))
        }
    }

    /* =========================
     * CompanyCandidate 생성
     * ========================= */

    @Nested
    @DisplayName("CompanyCandidate 생성 로직")
    inner class CompanyCandidateCreation {

        @Test
        fun `CompanyCandidate가 null이면 생성 안함`() {
            // Given
            val command = createValidCommand()
            val llmResult = createSuccessfulLlmResult(companyCandidate = null)

            setupSuccessfulPath(command = command, llmResult = llmResult)

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify(exactly = 0) {
                companyCandidateWriteService.createCandidate(any(), any(), any(), any(), any())
            }
        }

        @Test
        fun `CompanyCandidate가 빈 문자열이면 생성 안함`() {
            // Given
            val command = createValidCommand()
            val llmResult = createSuccessfulLlmResult(companyCandidate = "")

            setupSuccessfulPath(command = command, llmResult = llmResult)

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then
            verify(exactly = 0) {
                companyCandidateWriteService.createCandidate(any(), any(), any(), any(), any())
            }
        }

        @Test
        fun `CompanyCandidate 생성 실패해도 파이프라인은 성공`() {
            // Given
            val command = createValidCommand()
            val llmResult = createSuccessfulLlmResult(companyCandidate = "주식회사 테스트")

            setupSuccessfulPath(command = command, llmResult = llmResult)

            // CompanyCandidate 생성 실패하도록 설정
            every {
                companyCandidateWriteService.createCandidate(any(), any(), any(), any(), any())
            } throws RuntimeException("DB connection failed")

            // When
            pipeline.execute(command).get(5, TimeUnit.SECONDS)

            // Then - 파이프라인은 성공 처리됨
            verify(exactly = 1) { processingWriteService.markCompleted(any()) }
            verify(exactly = 0) { processingWriteService.markFailed(any(), any(), any()) }
        }
    }
}