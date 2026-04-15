package com.hirelog.api.job.application.rag

import com.hirelog.api.job.application.rag.executor.RagQueryExecutor
import com.hirelog.api.job.application.rag.model.RagFilters
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.model.RagQuery
import com.hirelog.api.job.application.rag.port.RagCohortQuery
import com.hirelog.api.job.application.rag.port.RagEmbedding
import com.hirelog.api.job.application.rag.port.RagLlmFeatureExtractor
import com.hirelog.api.job.application.rag.port.RagReviewRecord
import com.hirelog.api.job.application.rag.port.RagStageRecord
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter.AggregationResult
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter.KnnSearchResult
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter.RawDocumentFields
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RagQueryExecutor 테스트")
class RagQueryExecutorTest {

    private lateinit var executor: RagQueryExecutor
    private lateinit var ragEmbedding: RagEmbedding
    private lateinit var ragCohortQuery: RagCohortQuery
    private lateinit var ragLlmFeatureExtractor: RagLlmFeatureExtractor
    private lateinit var openSearchAdapter: JobSummaryOpenSearchAdapter

    private val memberId = 42L
    private val dummyVector = List(768) { 0.1f }

    @BeforeEach
    fun setUp() {
        ragEmbedding = mockk()
        ragCohortQuery = mockk()
        ragLlmFeatureExtractor = mockk()
        openSearchAdapter = mockk()

        executor = RagQueryExecutor(ragEmbedding, ragCohortQuery, ragLlmFeatureExtractor, openSearchAdapter)
    }

    // ─────────────────────────────────────────────────────────────
    // DOCUMENT_SEARCH / SUMMARY
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DOCUMENT_SEARCH intent는")
    inner class DocumentSearchTest {

        @Test
        @DisplayName("Hybrid 검색 결과를 RagDocument로 매핑하여 반환한다")
        fun shouldReturnDocumentsFromHybridSearch() {
            // Arrange
            val query = ragQuery(RagIntent.DOCUMENT_SEARCH, "Kafka 백엔드")
            val searchResults = listOf(
                knnResult(id = 1L, brandName = "토스", positionName = "Backend Engineer"),
                knnResult(id = 2L, brandName = "카카오", positionName = "서버 개발자")
            )
            every { ragEmbedding.embedQuery(any()) } returns dummyVector
            every { openSearchAdapter.searchHybrid(any(), any(), any(), any(), any(), any()) } returns searchResults

            // Act
            val context = executor.execute(query, memberId)

            // Assert
            assertThat(context.documents).hasSize(2)
            assertThat(context.documents[0].id).isEqualTo(1L)
            assertThat(context.documents[0].brandName).isEqualTo("토스")
            assertThat(context.documents[1].id).isEqualTo(2L)
            assertThat(context.aggregations).isEmpty()
            assertThat(context.textFeatures).isEmpty()
            assertThat(context.stageRecords).isEmpty()
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 documents를 반환한다")
        fun shouldReturnEmptyDocumentsWhenNoResults() {
            // Arrange
            val query = ragQuery(RagIntent.DOCUMENT_SEARCH, "희귀 기술스택")
            every { ragEmbedding.embedQuery(any()) } returns dummyVector
            every { openSearchAdapter.searchHybrid(any(), any(), any(), any(), any(), any()) } returns emptyList()

            // Act
            val context = executor.execute(query, memberId)

            // Assert
            assertThat(context.documents).isEmpty()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // STATISTICS
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("STATISTICS intent는")
    inner class StatisticsTest {

        @Test
        @DisplayName("cohort 조건 없으면 전체 aggregation만 반환하고 textFeatures는 비어있다")
        fun shouldReturnAggregationOnlyWhenNoCohortCondition() {
            // Arrange
            val query = ragQuery(RagIntent.STATISTICS)
            val aggResult = AggregationResult(
                techStacks = mapOf("Kafka" to 30L, "Spring Boot" to 20L),
                companyDomains = mapOf("FINTECH" to 15L),
                companySizes = mapOf("SCALE_UP" to 10L)
            )
            every { openSearchAdapter.aggregateFields(null, any(), any(), any()) } returns aggResult

            // Act
            val context = executor.execute(query, memberId)

            // Assert
            assertThat(context.aggregations).isNotEmpty()
            assertThat(context.textFeatures).isEmpty()
            assertThat(context.documents).isEmpty()

            val kafkaEntry = context.aggregations.find { it.label == "Kafka" }
            assertThat(kafkaEntry).isNotNull
            assertThat(kafkaEntry!!.cohortCount).isEqualTo(30L)
            assertThat(kafkaEntry.baselineMultiplier).isNull()
        }

        @Test
        @DisplayName("cohort 조건 있지만 결과가 없으면 빈 RagContext를 반환한다")
        fun shouldReturnEmptyContextWhenCohortIsEmpty() {
            // Arrange
            val query = ragQuery(RagIntent.STATISTICS, filters = RagFilters(saveType = MemberJobSummarySaveType.SAVED))
            every { ragCohortQuery.findJobSummaryIdsByCohort(memberId, MemberJobSummarySaveType.SAVED, null, null) } returns emptyList()

            // Act
            val context = executor.execute(query, memberId)

            // Assert
            assertThat(context.documents).isEmpty()
            assertThat(context.aggregations).isEmpty()
            assertThat(context.textFeatures).isEmpty()
        }

        @Test
        @DisplayName("baseline=true이면 전체 대비 배율(multiplier)을 계산한다")
        fun shouldCalculateBaselineMultiplierWhenBaselineIsTrue() {
            // Arrange
            // cohort: Kafka=8, Spring=4 (total=12)
            // baseline: Kafka=40, Spring=160 (total=200)
            // Kafka multiplier = (8/12) / (40/200) = 3.333...
            // Spring multiplier = (4/12) / (160/200) = 0.4167
            val query = ragQuery(RagIntent.STATISTICS, baseline = true,
                filters = RagFilters(saveType = MemberJobSummarySaveType.SAVED))
            val cohortIds = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L)
            val cohortAgg = AggregationResult(
                techStacks = mapOf("Kafka" to 8L, "Spring Boot" to 4L),
                companyDomains = emptyMap(),
                companySizes = emptyMap()
            )
            val baselineAgg = AggregationResult(
                techStacks = mapOf("Kafka" to 40L, "Spring Boot" to 160L),
                companyDomains = emptyMap(),
                companySizes = emptyMap()
            )
            every { ragCohortQuery.findJobSummaryIdsByCohort(memberId, MemberJobSummarySaveType.SAVED, null, null) } returns cohortIds
            every { openSearchAdapter.aggregateFields(cohortIds, any(), any(), any()) } returns cohortAgg
            every { openSearchAdapter.aggregateFields(null, any(), any(), any()) } returns baselineAgg
            every { openSearchAdapter.findCohortDocumentTexts(cohortIds) } returns emptyList()
            every { ragLlmFeatureExtractor.extractFeatureLabels(any()) } returns emptyList()

            // Act
            val context = executor.execute(query, memberId)

            // Assert
            val kafkaEntry = context.aggregations.find { it.label == "Kafka" && it.category == "techStack" }!!
            val springEntry = context.aggregations.find { it.label == "Spring Boot" && it.category == "techStack" }!!

            assertThat(kafkaEntry.baselineMultiplier).isCloseTo(3.333, within(0.01))
            assertThat(springEntry.baselineMultiplier).isCloseTo(0.417, within(0.01))
        }

        @Test
        @DisplayName("baseline에 없는 label은 multiplier=null로 설정된다")
        fun shouldSetNullMultiplierWhenLabelAbsentInBaseline() {
            // Arrange
            val query = ragQuery(RagIntent.STATISTICS, baseline = true,
                filters = RagFilters(saveType = MemberJobSummarySaveType.SAVED))
            val cohortIds = listOf(1L)
            val cohortAgg = AggregationResult(
                techStacks = mapOf("Rust" to 3L),  // Rust는 baseline에 없음
                companyDomains = emptyMap(),
                companySizes = emptyMap()
            )
            val baselineAgg = AggregationResult(
                techStacks = mapOf("Kafka" to 100L),  // Rust 없음
                companyDomains = emptyMap(),
                companySizes = emptyMap()
            )
            every { ragCohortQuery.findJobSummaryIdsByCohort(memberId, MemberJobSummarySaveType.SAVED, null, null) } returns cohortIds
            every { openSearchAdapter.aggregateFields(cohortIds, any(), any(), any()) } returns cohortAgg
            every { openSearchAdapter.aggregateFields(null, any(), any(), any()) } returns baselineAgg
            every { openSearchAdapter.findCohortDocumentTexts(cohortIds) } returns emptyList()
            every { ragLlmFeatureExtractor.extractFeatureLabels(any()) } returns emptyList()

            // Act
            val context = executor.execute(query, memberId)

            // Assert
            val rustEntry = context.aggregations.find { it.label == "Rust" }!!
            assertThat(rustEntry.baselineMultiplier).isNull()
        }

        @Test
        @DisplayName("cohort 문서에서 feature label 매칭 시 observedCount와 snippets를 정확히 계산한다")
        fun shouldBuildTextFeaturesWithCorrectCountAndSnippets() {
            // Arrange
            val query = ragQuery(RagIntent.STATISTICS, filters = RagFilters(saveType = MemberJobSummarySaveType.SAVED))
            val cohortIds = listOf(10L, 20L, 30L)
            val rawDocs = listOf(
                RawDocumentFields(
                    id = 10L,
                    responsibilities = "대용량 트래픽 처리 및 성능 최적화",
                    requiredQualifications = "3년 이상 경력",
                    preferredQualifications = null,
                    techStackParsed = null
                ),
                RawDocumentFields(
                    id = 20L,
                    responsibilities = "MSA 기반 서비스 개발",  // "대용량 트래픽" 없음
                    requiredQualifications = "Java 경력자",
                    preferredQualifications = null,
                    techStackParsed = null
                ),
                RawDocumentFields(
                    id = 30L,
                    responsibilities = "대용량 트래픽 환경에서 API 개발",
                    requiredQualifications = "Spring 경력",
                    preferredQualifications = null,
                    techStackParsed = null
                )
            )
            val aggResult = AggregationResult(emptyMap(), emptyMap(), emptyMap())
            every { ragCohortQuery.findJobSummaryIdsByCohort(memberId, MemberJobSummarySaveType.SAVED, null, null) } returns cohortIds
            every { openSearchAdapter.aggregateFields(cohortIds, any(), any(), any()) } returns aggResult
            every { openSearchAdapter.findCohortDocumentTexts(cohortIds) } returns rawDocs
            every { ragLlmFeatureExtractor.extractFeatureLabels(any()) } returns listOf("대용량 트래픽")

            // Act
            val context = executor.execute(query, memberId)

            // Assert
            assertThat(context.textFeatures).hasSize(1)
            val feature = context.textFeatures[0]
            assertThat(feature.feature).isEqualTo("대용량 트래픽")
            assertThat(feature.observedCount).isEqualTo(2)     // doc 10, 30에서 매칭
            assertThat(feature.sourceIds).containsExactlyInAnyOrder(10L, 30L)
            assertThat(feature.snippets).isNotEmpty()
        }

        @Test
        @DisplayName("feature label이 어느 문서에도 없으면 해당 feature는 결과에서 제외된다")
        fun shouldExcludeFeatureLabelNotFoundInAnyDocument() {
            // Arrange
            val query = ragQuery(RagIntent.STATISTICS, filters = RagFilters(saveType = MemberJobSummarySaveType.SAVED))
            val cohortIds = listOf(1L)
            val rawDocs = listOf(
                RawDocumentFields(
                    id = 1L,
                    responsibilities = "서버 개발",
                    requiredQualifications = "Java",
                    preferredQualifications = null,
                    techStackParsed = null
                )
            )
            val aggResult = AggregationResult(emptyMap(), emptyMap(), emptyMap())
            every { ragCohortQuery.findJobSummaryIdsByCohort(memberId, MemberJobSummarySaveType.SAVED, null, null) } returns cohortIds
            every { openSearchAdapter.aggregateFields(cohortIds, any(), any(), any()) } returns aggResult
            every { openSearchAdapter.findCohortDocumentTexts(cohortIds) } returns rawDocs
            every { ragLlmFeatureExtractor.extractFeatureLabels(any()) } returns listOf("대용량 트래픽")

            // Act
            val context = executor.execute(query, memberId)

            // Assert — "대용량 트래픽"은 문서에 없으므로 textFeatures 비어있음
            assertThat(context.textFeatures).isEmpty()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // EXPERIENCE_ANALYSIS
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EXPERIENCE_ANALYSIS intent는")
    inner class ExperienceAnalysisTest {

        @Test
        @DisplayName("HiringStageRecord와 JobSummaryReview를 함께 반환한다")
        fun shouldReturnStageRecordsAndReviewRecordsFromDb() {
            // Arrange
            val query = ragQuery(RagIntent.EXPERIENCE_ANALYSIS)
            val records = listOf(
                RagStageRecord(
                    brandName = "토스",
                    positionName = "Backend Engineer",
                    stage = "INTERVIEW_1",
                    note = "알고리즘 위주 면접",
                    result = "PASSED"
                ),
                RagStageRecord(
                    brandName = "카카오",
                    positionName = "서버 개발자",
                    stage = "INTERVIEW_2",
                    note = "시스템 설계 질문",
                    result = "FAILED"
                )
            )
            val reviews = listOf(
                RagReviewRecord(
                    brandName = "토스",
                    positionName = "Backend Engineer",
                    hiringStage = "INTERVIEW_1",
                    difficultyRating = 7,
                    satisfactionRating = 9,
                    prosComment = "자유로운 분위기",
                    consComment = "프로세스가 빠름",
                    tip = "알고리즘 준비 필수"
                )
            )
            every { ragCohortQuery.findStageRecordsForRag(memberId, null, null) } returns records
            every { ragCohortQuery.findReviewsByMemberId(memberId) } returns reviews

            // Act
            val context = executor.execute(query, memberId)

            // Assert
            assertThat(context.stageRecords).hasSize(2)
            assertThat(context.stageRecords[0].brandName).isEqualTo("토스")
            assertThat(context.stageRecords[1].result).isEqualTo("FAILED")
            assertThat(context.reviewRecords).hasSize(1)
            assertThat(context.reviewRecords[0].prosComment).isEqualTo("자유로운 분위기")
            assertThat(context.documents).isEmpty()
            assertThat(context.aggregations).isEmpty()
        }

        @Test
        @DisplayName("경험 기록이 없어도 리뷰 레코드는 독립적으로 반환된다")
        fun shouldReturnEmptyStageRecordsWhenNoneExist() {
            // Arrange
            val query = ragQuery(RagIntent.EXPERIENCE_ANALYSIS)
            every { ragCohortQuery.findStageRecordsForRag(memberId, null, null) } returns emptyList()
            every { ragCohortQuery.findReviewsByMemberId(memberId) } returns emptyList()

            // Act
            val context = executor.execute(query, memberId)

            // Assert
            assertThat(context.stageRecords).isEmpty()
            assertThat(context.reviewRecords).isEmpty()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 픽스처 헬퍼
    // ─────────────────────────────────────────────────────────────

    private fun ragQuery(
        intent: RagIntent,
        parsedText: String = "테스트 질문",
        baseline: Boolean = false,
        filters: RagFilters = RagFilters()
    ) = RagQuery(
        intent = intent,
        semanticRetrieval = intent == RagIntent.DOCUMENT_SEARCH || intent == RagIntent.SUMMARY,
        aggregation = intent == RagIntent.STATISTICS,
        baseline = baseline,
        filters = filters,
        parsedText = parsedText
    )

    private fun knnResult(id: Long, brandName: String, positionName: String) = KnnSearchResult(
        id = id,
        score = 0.9f,
        brandName = brandName,
        positionName = positionName,
        companyDomain = "FINTECH",
        companySize = "SCALE_UP",
        responsibilities = "서버 개발",
        requiredQualifications = "3년 이상",
        preferredQualifications = null,
        techStackParsed = listOf("Kotlin", "Spring Boot"),
        idealCandidate = null,
        mustHaveSignals = null,
        technicalContext = null,
        preparationFocus = null
    )
}
