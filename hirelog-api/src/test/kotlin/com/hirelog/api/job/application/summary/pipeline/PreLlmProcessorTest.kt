package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.application.view.CompanyNameView
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.intake.model.DuplicateReason
import com.hirelog.api.job.application.intake.model.IntakeHashes
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.snapshot.JobSnapshotWriteService
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.domain.type.RecruitmentPeriodType
import com.hirelog.api.position.application.port.PositionQuery
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.util.UUID

@DisplayName("PreLlmProcessor 테스트")
class PreLlmProcessorTest {

    private lateinit var processor: PreLlmProcessor
    private lateinit var processingWriteService: JdSummaryProcessingWriteService
    private lateinit var snapshotWriteService: JobSnapshotWriteService
    private lateinit var jdIntakePolicy: JdIntakePolicy
    private lateinit var summaryQuery: JobSummaryQuery
    private lateinit var positionQuery: PositionQuery
    private lateinit var companyQuery: CompanyQuery

    private val processingId = UUID.randomUUID()
    private val baseCommand = JobSummaryGenerateCommand(
        requestId = processingId.toString(),
        brandName = "Toss",
        positionName = "Backend Engineer",
        source = JobSourceType.TEXT,
        sourceUrl = null,
        canonicalMap = mapOf("raw" to listOf("JD 내용")),
        recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
        openedDate = null,
        closedDate = null,
        skills = emptyList(),
        occurredAt = System.currentTimeMillis()
    )
    private val hashes = IntakeHashes(canonicalHash = "abc123", simHash = 123456L, coreText = "core")

    @BeforeEach
    fun setUp() {
        processingWriteService = mockk(relaxed = true)
        snapshotWriteService = mockk(relaxed = true)
        jdIntakePolicy = mockk()
        summaryQuery = mockk()
        positionQuery = mockk()
        companyQuery = mockk()
        processor = PreLlmProcessor(
            processingWriteService, snapshotWriteService, jdIntakePolicy,
            summaryQuery, positionQuery, companyQuery
        )
    }

    @Nested
    @DisplayName("execute 메서드는")
    inner class ExecuteTest {

        @Test
        @DisplayName("유효하지 않은 JD이면 FAILED로 전이하고 null을 반환한다")
        fun shouldReturnNullWhenInvalidJd() {
            every { jdIntakePolicy.isValidJd(baseCommand) } returns false

            val result = processor.execute(processingId, baseCommand)

            assertThat(result).isNull()
            verify { processingWriteService.markFailed(processingId, "INVALID_INPUT", any()) }
        }

        @Test
        @DisplayName("URL 기반 JD에서 중복 URL이면 DUPLICATE로 전이하고 null을 반환한다")
        fun shouldReturnNullWhenUrlDuplicate() {
            val urlCommand = baseCommand.copy(
                source = JobSourceType.URL,
                sourceUrl = "https://example.com/jd/123"
            )

            every { jdIntakePolicy.isValidJd(urlCommand) } returns true
            every { summaryQuery.existsBySourceUrl("https://example.com/jd/123") } returns true

            val result = processor.execute(processingId, urlCommand)

            assertThat(result).isNull()
            verify { processingWriteService.markDuplicate(processingId, "URL_DUPLICATE") }
        }

        @Test
        @DisplayName("Hash 중복이면 DUPLICATE로 전이하고 null을 반환한다")
        fun shouldReturnNullWhenHashDuplicate() {
            every { jdIntakePolicy.isValidJd(baseCommand) } returns true
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(baseCommand, hashes) } returns
                DuplicateDecision.Duplicate(DuplicateReason.HASH, 10L, 20L)

            val result = processor.execute(processingId, baseCommand)

            assertThat(result).isNull()
            verify { processingWriteService.markDuplicate(processingId, "HASH") }
        }

        @Test
        @DisplayName("재처리 대상이면 기존 snapshotId로 SUMMARIZING 전이 후 결과를 반환한다")
        fun shouldReturnResultWhenReprocessable() {
            every { jdIntakePolicy.isValidJd(baseCommand) } returns true
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(baseCommand, hashes) } returns
                DuplicateDecision.Reprocessable(existingSnapshotId = 55L)
            every { positionQuery.findActiveNames() } returns listOf("Backend", "Frontend")
            every { companyQuery.findAllNames() } returns listOf(CompanyNameView(id = 1L, name = "Toss"))

            val result = processor.execute(processingId, baseCommand)

            assertThat(result).isNotNull()
            assertThat(result!!.snapshotId).isEqualTo(55L)
            verify { processingWriteService.markSummarizing(processingId, 55L) }
        }

        @Test
        @DisplayName("신규 JD이면 Snapshot을 생성하고 SUMMARIZING 전이 후 결과를 반환한다")
        fun shouldRecordSnapshotAndReturnResultWhenNotDuplicate() {
            every { jdIntakePolicy.isValidJd(baseCommand) } returns true
            every { jdIntakePolicy.generateIntakeHashes(any()) } returns hashes
            every { jdIntakePolicy.decideDuplicate(baseCommand, hashes) } returns DuplicateDecision.NotDuplicate
            every { snapshotWriteService.record(any()) } returns 100L
            every { positionQuery.findActiveNames() } returns listOf("Backend")
            every { companyQuery.findAllNames() } returns emptyList()

            val result = processor.execute(processingId, baseCommand)

            assertThat(result).isNotNull()
            assertThat(result!!.snapshotId).isEqualTo(100L)
            verify { snapshotWriteService.record(any()) }
            verify { processingWriteService.markSummarizing(processingId, 100L) }
        }
    }
}
