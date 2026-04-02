package com.hirelog.api.job.application.summary

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.pipeline.LlmInvocationService
import com.hirelog.api.job.application.summary.pipeline.PipelineErrorHandler
import com.hirelog.api.job.application.summary.pipeline.PostLlmProcessor
import com.hirelog.api.job.application.summary.pipeline.PreLlmProcessor
import com.hirelog.api.job.application.summary.view.JobSummaryInsightResult
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.model.JdSummaryProcessing
import com.hirelog.api.job.domain.type.CareerType
import com.hirelog.api.job.domain.type.JdSummaryProcessingStatus
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.domain.type.RecruitmentPeriodType
import com.hirelog.api.common.domain.LlmProvider
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@DisplayName("JdSummaryGenerationFacade 테스트")
class JdSummaryGenerationFacadeTest {

    private lateinit var facade: JdSummaryGenerationFacade
    private lateinit var preLlm: PreLlmProcessor
    private lateinit var llmInvoker: LlmInvocationService
    private lateinit var postLlm: PostLlmProcessor
    private lateinit var errorHandler: PipelineErrorHandler
    private lateinit var processingWriteService: JdSummaryProcessingWriteService
    private lateinit var processingQuery: JdSummaryProcessingQuery
    private lateinit var objectMapper: ObjectMapper
    private lateinit var eventPublisher: ApplicationEventPublisher

    // 동기 실행 Executor (테스트에서 비동기 제어용)
    private val syncExecutor = Executor { it.run() }

    private val processingId = UUID.randomUUID()
    private val command = JobSummaryGenerateCommand(
        requestId = processingId.toString(),
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
        preLlm = mockk()
        llmInvoker = mockk()
        postLlm = mockk(relaxed = true)
        errorHandler = mockk(relaxed = true)
        processingWriteService = mockk(relaxed = true)
        processingQuery = mockk()
        objectMapper = mockk()
        eventPublisher = mockk(relaxed = true)

        val processing = mockk<JdSummaryProcessing>(relaxed = true)
        every { processing.id } returns processingId
        every { processing.status } returns JdSummaryProcessingStatus.RECEIVED

        every { processingQuery.findById(processingId) } returns processing
        every { objectMapper.writeValueAsString(any()) } returns "{}"

        facade = JdSummaryGenerationFacade(
            preLlm, llmInvoker, postLlm, errorHandler,
            processingWriteService, processingQuery, objectMapper, eventPublisher, syncExecutor
        )
    }

    @Nested
    @DisplayName("execute는")
    inner class ExecuteTest {

        @Test
        @DisplayName("preLlm이 null을 반환하면 LLM을 호출하지 않고 완료된 Future를 반환한다")
        fun shouldSkipLlmWhenPreLlmReturnsNull() {
            every { preLlm.execute(processingId, command) } returns null

            val future = facade.execute(command)
            future.get() // 완료 대기

            verify(exactly = 0) { llmInvoker.invoke(any(), any(), any()) }
            verify(exactly = 0) { errorHandler.handle(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("preLlm에서 예외 발생 시 errorHandler를 호출하고 Future를 반환한다")
        fun shouldCallErrorHandlerWhenPreLlmThrows() {
            every { preLlm.execute(processingId, command) } throws RuntimeException("pre-llm error")

            val future = facade.execute(command)
            future.get()

            verify { errorHandler.handle(processingId, any(), command.requestId, "PRE_LLM") }
            verify(exactly = 0) { llmInvoker.invoke(any(), any(), any()) }
        }

        @Test
        @DisplayName("정상 흐름에서 LLM 결과를 저장하고 postLlm을 호출한다")
        fun shouldExecuteFullPipelineOnSuccess() {
            val preResult = PreLlmProcessor.PreLlmResult(
                snapshotId = 10L,
                positionCandidates = listOf("Backend"),
                existCompanies = listOf("토스")
            )

            every { preLlm.execute(processingId, command) } returns preResult
            every {
                llmInvoker.invoke(command, preResult.positionCandidates, preResult.existCompanies)
            } returns CompletableFuture.completedFuture(llmResult)

            val future = facade.execute(command)
            future.get()

            verify { processingWriteService.saveLlmResult(processingId, "{}", "Toss", "Backend Engineer") }
            verify { postLlm.execute(10L, llmResult, processingId, command) }
            verify(exactly = 0) { errorHandler.handle(any(), any(), any(), "LLM_OR_POST_LLM") }
        }

        @Test
        @DisplayName("LLM 호출 실패 시 exceptionally에서 errorHandler를 호출한다")
        fun shouldCallErrorHandlerWhenLlmFails() {
            val preResult = PreLlmProcessor.PreLlmResult(
                snapshotId = 10L,
                positionCandidates = emptyList(),
                existCompanies = emptyList()
            )

            every { preLlm.execute(processingId, command) } returns preResult
            every {
                llmInvoker.invoke(command, any(), any())
            } returns CompletableFuture.failedFuture(RuntimeException("LLM timeout"))

            val future = facade.execute(command)
            future.get()

            verify { errorHandler.handle(processingId, any(), command.requestId, "LLM") }
        }
    }
}
