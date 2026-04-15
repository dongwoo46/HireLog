package com.hirelog.api.job.application.summary

import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.intake.model.DuplicateReason
import com.hirelog.api.job.application.intake.model.IntakeHashes
import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.application.summary.payload.JobSummarySearchPayload
import com.hirelog.api.job.application.summary.pipeline.PostLlmProcessor
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.application.summary.port.JobSummaryEmbedding
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.domain.model.JobSummary
import com.hirelog.api.job.domain.model.JobSummaryInsight
import com.hirelog.api.job.domain.type.CareerType
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexManager
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter
import com.hirelog.api.position.application.port.PositionQuery
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.nio.file.AccessDeniedException
import java.time.LocalDateTime

@DisplayName("JobSummaryAdminService 테스트")
class JobSummaryAdminServiceTest {

    private lateinit var service: JobSummaryAdminService
    private lateinit var jdIntakePolicy: JdIntakePolicy
    private lateinit var snapshotCommand: JobSnapshotCommand
    private lateinit var llmClient: JobSummaryLlm
    private lateinit var summaryQuery: JobSummaryQuery
    private lateinit var positionQuery: PositionQuery
    private lateinit var companyQuery: CompanyQuery
    private lateinit var postLlmProcessor: PostLlmProcessor
    private lateinit var openSearchAdapter: JobSummaryOpenSearchAdapter
    private lateinit var indexManager: JobSummaryIndexManager
    private lateinit var summaryCommand: JobSummaryCommand
    private lateinit var embeddingPort: JobSummaryEmbedding

    private val adminPassword = "test-admin-password"

    @BeforeEach
    fun setUp() {
        jdIntakePolicy = mockk()
        snapshotCommand = mockk()
        llmClient = mockk()
        summaryQuery = mockk()
        positionQuery = mockk()
        companyQuery = mockk()
        postLlmProcessor = mockk()
        openSearchAdapter = mockk()
        indexManager = mockk()
        summaryCommand = mockk()
        embeddingPort = mockk()

        service = JobSummaryAdminService(
            jdIntakePolicy, snapshotCommand, llmClient,
            summaryQuery, positionQuery, companyQuery,
            postLlmProcessor,
            openSearchAdapter, indexManager, summaryCommand, embeddingPort,
            adminPassword
        )
    }

    // ───────────────────────────────────────────────────────────────
    // 테스트 픽스처 헬퍼
    // ───────────────────────────────────────────────────────────────

    /**
     * id를 제어해야 하는 경우 (커서 기반 페이징 테스트 등)에 사용.
     * JobSummary는 protected constructor → JPA가 id를 부여하므로
     * 테스트에서 id를 지정하려면 mockk을 사용한다.
     */
    private fun createSummaryMock(id: Long): JobSummary = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { careerType } returns CareerType.EXPERIENCED
        every { createdAt } returns LocalDateTime.of(2026, 4, 13, 12, 0, 0)
        every { insight } returns JobSummaryInsight.empty()
    }

    private fun createEmbeddingCandidate(id: Long) = JobSummaryOpenSearchAdapter.EmbeddingCandidate(
        id = id,
        responsibilities = "핵심 업무 $id",
        requiredQualifications = "필수 요건 $id",
        preferredQualifications = null,
        idealCandidate = null,
        mustHaveSignals = null,
        technicalContext = null
    )

    // ───────────────────────────────────────────────────────────────
    // verify 테스트
    // ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verify 메서드는")
    inner class VerifyTest {

        @Test
        @DisplayName("올바른 비밀번호이면 예외 없이 통과한다")
        fun shouldPassWithCorrectPassword() {
            service.verify(adminPassword)
        }

        @Test
        @DisplayName("잘못된 비밀번호이면 AccessDeniedException을 던진다")
        fun shouldThrowWithWrongPassword() {
            assertThatThrownBy {
                service.verify("wrong-password")
            }.isInstanceOf(AccessDeniedException::class.java)
        }
    }

    // ───────────────────────────────────────────────────────────────
    // createDirectly 테스트
    // ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createDirectly 메서드는")
    inner class CreateDirectlyTest {

        @Test
        @DisplayName("sourceUrl 중복이면 IllegalStateException을 던진다")
        fun shouldThrowWhenSourceUrlDuplicate() {
            every { summaryQuery.existsBySourceUrl("https://example.com/jd/1") } returns true

            assertThatThrownBy {
                service.createDirectly(
                    brandName = "Toss",
                    positionName = "Backend Engineer",
                    jdText = "JD 내용입니다.",
                    sourceUrl = "https://example.com/jd/1"
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("sourceUrl already exists")
        }

        @Test
        @DisplayName("Hash 중복이면 IllegalStateException을 던진다")
        fun shouldThrowWhenHashDuplicate() {
            val hashes = IntakeHashes("hash123", 111L, "core text")

            every { summaryQuery.existsBySourceUrl(any()) } returns false
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.findHashDuplicate("hash123") } returns
                DuplicateDecision.Duplicate(DuplicateReason.HASH, 10L, 20L)

            assertThatThrownBy {
                service.createDirectly(
                    brandName = "Toss",
                    positionName = "Backend Engineer",
                    jdText = "JD 내용입니다.",
                    sourceUrl = null
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("hash duplicate")
        }
    }

    // ───────────────────────────────────────────────────────────────
    // reindexAll 테스트
    // ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reindexAll 메서드는")
    inner class ReindexAllTest {

        @Test
        @DisplayName("batchSize가 0이면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenBatchSizeIsZero() {
            assertThatThrownBy { service.reindexAll(batchSize = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("batchSize must be between 1 and 200")
        }

        @Test
        @DisplayName("batchSize가 201이면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenBatchSizeExceedsMax() {
            assertThatThrownBy { service.reindexAll(batchSize = 201) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("batchSize must be between 1 and 200")
        }

        @Test
        @DisplayName("DB가 비어있으면 인덱스를 재생성하고 0을 반환한다")
        fun shouldRecreateIndexAndReturnZeroWhenDbIsEmpty() {
            // Arrange
            every { indexManager.deleteIndex() } just Runs
            every { indexManager.createIndexIfNotExists() } just Runs
            every { summaryCommand.findAllForReindex(0L, 50) } returns emptyList()

            // Act
            val result = service.reindexAll(batchSize = 50)

            // Assert
            assertThat(result).isZero()
            verify(exactly = 1) { indexManager.deleteIndex() }
            verify(exactly = 1) { indexManager.createIndexIfNotExists() }
        }

        @Test
        @DisplayName("임베딩 성공 시 벡터와 함께 인덱싱하고 성공 건수를 반환한다")
        fun shouldIndexWithVectorAndReturnSuccessCount() {
            // Arrange
            val expectedVector = listOf(0.1f, 0.2f, 0.3f)
            val summary1 = createSummaryMock(id = 1L)
            val summary2 = createSummaryMock(id = 2L)

            every { indexManager.deleteIndex() } just Runs
            every { indexManager.createIndexIfNotExists() } just Runs
            every { summaryCommand.findAllForReindex(0L, 10) } returns listOf(summary1, summary2)
            every { embeddingPort.embed(any()) } returns expectedVector
            val indexSlot = mutableListOf<JobSummarySearchPayload>()
            every { openSearchAdapter.index(capture(indexSlot)) } just Runs

            // Act
            val result = service.reindexAll(batchSize = 10)

            // Assert
            assertThat(result).isEqualTo(2)
            assertThat(indexSlot).hasSize(2)
            assertThat(indexSlot.first().embeddingVector).isEqualTo(expectedVector)
        }

        @Test
        @DisplayName("임베딩 실패한 문서는 건너뛰고 나머지 성공 건수를 반환한다")
        fun shouldSkipFailedDocumentAndReturnPartialSuccessCount() {
            // Arrange
            val summary1 = createSummaryMock(id = 1L)
            val summary2 = createSummaryMock(id = 2L)

            every { indexManager.deleteIndex() } just Runs
            every { indexManager.createIndexIfNotExists() } just Runs
            every { summaryCommand.findAllForReindex(0L, 10) } returns listOf(summary1, summary2)
            every { embeddingPort.embed(any()) } throwsMany listOf(
                RuntimeException("임베딩 서버 오류"),
                RuntimeException("임베딩 서버 오류")
            )
            every { openSearchAdapter.index(any()) } just Runs

            // Act
            val result = service.reindexAll(batchSize = 10)

            // Assert — 임베딩 실패 시 openSearchAdapter.index()가 호출되지 않으므로 성공 건수 0
            assertThat(result).isZero()
        }

        @Test
        @DisplayName("커서 기반으로 배치를 순차 처리하고 총 성공 건수를 반환한다")
        fun shouldProcessBatchesSequentiallyWithCursorAndReturnTotalCount() {
            // Arrange
            val batchSize = 2
            val batch1 = listOf(createSummaryMock(id = 5L), createSummaryMock(id = 10L))
            val batch2 = listOf(createSummaryMock(id = 15L))

            every { indexManager.deleteIndex() } just Runs
            every { indexManager.createIndexIfNotExists() } just Runs
            // 첫 번째 배치: lastId=0 → [id=5, id=10]
            every { summaryCommand.findAllForReindex(0L, batchSize) } returns batch1
            // 두 번째 배치: lastId=10 → [id=15] (size=1 < batchSize=2 → loop 종료)
            every { summaryCommand.findAllForReindex(10L, batchSize) } returns batch2
            every { embeddingPort.embed(any()) } returns listOf(0.1f, 0.2f)
            every { openSearchAdapter.index(any()) } just Runs

            // Act
            val result = service.reindexAll(batchSize = batchSize)

            // Assert
            assertThat(result).isEqualTo(3)
            verify(exactly = 1) { summaryCommand.findAllForReindex(0L, batchSize) }
            verify(exactly = 1) { summaryCommand.findAllForReindex(10L, batchSize) }
        }
    }

    // ───────────────────────────────────────────────────────────────
    // reindexMissingEmbeddings 테스트
    // ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reindexMissingEmbeddings 메서드는")
    inner class ReindexMissingEmbeddingsTest {

        @Test
        @DisplayName("batchSize가 0이면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenBatchSizeIsZero() {
            assertThatThrownBy { service.reindexMissingEmbeddings(batchSize = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("batchSize must be between 1 and 500")
        }

        @Test
        @DisplayName("batchSize가 501이면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenBatchSizeExceedsMax() {
            assertThatThrownBy { service.reindexMissingEmbeddings(batchSize = 501) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("batchSize must be between 1 and 500")
        }

        @Test
        @DisplayName("누락 문서가 없으면 0을 반환한다")
        fun shouldReturnZeroWhenNoCandidates() {
            // Arrange
            every { openSearchAdapter.findMissingEmbedding(100) } returns emptyList()

            // Act
            val result = service.reindexMissingEmbeddings(batchSize = 100)

            // Assert
            assertThat(result).isZero()
            verify(exactly = 0) { openSearchAdapter.updateEmbeddingVector(any(), any()) }
        }

        @Test
        @DisplayName("임베딩 성공 시 벡터를 업데이트하고 성공 건수를 반환한다")
        fun shouldUpdateVectorAndReturnSuccessCount() {
            // Arrange
            val expectedVector = listOf(0.5f, 0.6f, 0.7f)
            val candidates = listOf(createEmbeddingCandidate(1L), createEmbeddingCandidate(2L))

            every { openSearchAdapter.findMissingEmbedding(100) } returns candidates
            every { embeddingPort.embed(any()) } returns expectedVector
            every { openSearchAdapter.updateEmbeddingVector(any(), expectedVector) } just Runs

            // Act
            val result = service.reindexMissingEmbeddings(batchSize = 100)

            // Assert
            assertThat(result).isEqualTo(2)
            verify(exactly = 1) { openSearchAdapter.updateEmbeddingVector(1L, expectedVector) }
            verify(exactly = 1) { openSearchAdapter.updateEmbeddingVector(2L, expectedVector) }
        }

        @Test
        @DisplayName("임베딩 실패한 문서는 건너뛰고 나머지 성공 건수를 반환한다")
        fun shouldSkipFailedDocumentAndReturnPartialSuccessCount() {
            // Arrange
            val expectedVector = listOf(0.5f, 0.6f, 0.7f)
            val candidates = listOf(createEmbeddingCandidate(1L), createEmbeddingCandidate(2L))

            every { openSearchAdapter.findMissingEmbedding(100) } returns candidates
            // id=1 실패, id=2 성공
            every { embeddingPort.embed(match { it.responsibilities.endsWith("1") }) } throws
                RuntimeException("임베딩 서버 오류")
            every { embeddingPort.embed(match { it.responsibilities.endsWith("2") }) } returns expectedVector
            every { openSearchAdapter.updateEmbeddingVector(2L, expectedVector) } just Runs

            // Act
            val result = service.reindexMissingEmbeddings(batchSize = 100)

            // Assert
            assertThat(result).isEqualTo(1)
            verify(exactly = 0) { openSearchAdapter.updateEmbeddingVector(1L, any()) }
            verify(exactly = 1) { openSearchAdapter.updateEmbeddingVector(2L, expectedVector) }
        }
    }
}