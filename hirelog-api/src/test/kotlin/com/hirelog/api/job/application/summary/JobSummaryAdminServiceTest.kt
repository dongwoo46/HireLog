package com.hirelog.api.job.application.summary

import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.intake.model.DuplicateReason
import com.hirelog.api.job.application.intake.model.IntakeHashes
import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.application.summary.pipeline.PostLlmProcessor
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.position.application.port.PositionQuery
import io.mockk.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.nio.file.AccessDeniedException

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

        service = JobSummaryAdminService(
            jdIntakePolicy, snapshotCommand, llmClient,
            summaryQuery, positionQuery, companyQuery,
            postLlmProcessor, adminPassword
        )
    }

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
}
