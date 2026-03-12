package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryInsightResult
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.type.CareerType
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.domain.type.RecruitmentPeriodType
import com.hirelog.api.common.domain.LlmProvider
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

@DisplayName("LlmInvocationService 테스트")
class LlmInvocationServiceTest {

    private lateinit var service: LlmInvocationService
    private lateinit var primary: JobSummaryLlm
    private lateinit var fallback: JobSummaryLlm

    private val command = JobSummaryGenerateCommand(
        requestId = "req-001",
        brandName = "Toss",
        positionName = "Backend Engineer",
        source = JobSourceType.TEXT,
        sourceUrl = null,
        canonicalMap = mapOf("responsibilities" to listOf("서버 개발")),
        recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
        openedDate = null,
        closedDate = null,
        skills = emptyList(),
        occurredAt = System.currentTimeMillis()
    )

    private val llmResult = JobSummaryLlmResult(
        llmProvider = LlmProvider.GEMINI,
        brandName = "Toss",
        positionName = "Backend Engineer",
        companyCandidate = null,
        careerType = CareerType.EXPERIENCED,
        careerYears = "3년 이상",
        summary = "요약",
        responsibilities = "업무",
        requiredQualifications = "자격",
        preferredQualifications = null,
        techStack = null,
        recruitmentProcess = null,
        insight = JobSummaryInsightResult(
            idealCandidate = null, mustHaveSignals = null, preparationFocus = null,
            transferableStrengthsAndGapPlan = null, proofPointsAndMetrics = null,
            storyAngles = null, keyChallenges = null, technicalContext = null,
            questionsToAsk = null, considerations = null
        )
    )

    @BeforeEach
    fun setUp() {
        primary = mockk()
        fallback = mockk()
        service = LlmInvocationService(primary, fallback)
    }

    @Nested
    @DisplayName("invoke는")
    inner class InvokeTest {

        @Test
        @DisplayName("primary 성공 시 primary 결과를 반환한다")
        fun shouldReturnPrimaryResultWhenSuccess() {
            every {
                primary.summarizeJobDescriptionAsync(any(), any(), any(), any(), any())
            } returns CompletableFuture.completedFuture(llmResult)

            val result = service.invoke(command, listOf("Backend"), listOf("토스")).get()

            assertThat(result).isEqualTo(llmResult)
            verify(exactly = 0) { fallback.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("primary 실패 시 fallback을 호출하고 결과를 반환한다")
        fun shouldUseFallbackWhenPrimaryFails() {
            val failedFuture = CompletableFuture.failedFuture<JobSummaryLlmResult>(
                RuntimeException("Gemini API error")
            )
            val fallbackResult = llmResult.copy(llmProvider = LlmProvider.OPENAI)

            every {
                primary.summarizeJobDescriptionAsync(any(), any(), any(), any(), any())
            } returns failedFuture
            every {
                fallback.summarizeJobDescriptionAsync(any(), any(), any(), any(), any())
            } returns CompletableFuture.completedFuture(fallbackResult)

            val result = service.invoke(command, listOf("Backend"), listOf("토스")).get()

            assertThat(result).isEqualTo(fallbackResult)
            verify { fallback.summarizeJobDescriptionAsync(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("primary와 fallback 모두 실패하면 ExecutionException을 던진다")
        fun shouldThrowWhenBothFail() {
            val primaryFailed = CompletableFuture.failedFuture<JobSummaryLlmResult>(
                RuntimeException("primary error")
            )
            val fallbackFailed = CompletableFuture.failedFuture<JobSummaryLlmResult>(
                RuntimeException("fallback error")
            )

            every {
                primary.summarizeJobDescriptionAsync(any(), any(), any(), any(), any())
            } returns primaryFailed
            every {
                fallback.summarizeJobDescriptionAsync(any(), any(), any(), any(), any())
            } returns fallbackFailed

            assertThatThrownBy {
                service.invoke(command, emptyList(), emptyList()).get()
            }.isInstanceOf(ExecutionException::class.java)
                .hasCauseInstanceOf(RuntimeException::class.java)
        }
    }
}
