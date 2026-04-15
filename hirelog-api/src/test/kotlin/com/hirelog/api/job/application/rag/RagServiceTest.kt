package com.hirelog.api.job.application.rag

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.job.application.rag.executor.RagQueryExecutor
import com.hirelog.api.job.application.rag.model.RagAnswer
import com.hirelog.api.job.application.rag.model.RagFilters
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.model.RagQuery
import com.hirelog.api.job.application.rag.port.RagContext
import com.hirelog.api.job.application.rag.port.RagLlmComposer
import com.hirelog.api.job.application.rag.port.RagLlmParser
import com.hirelog.api.job.application.rag.port.RagQueryLogCommand
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RagService 테스트")
class RagServiceTest {

    private lateinit var ragLlmParser: RagLlmParser
    private lateinit var ragQueryExecutor: RagQueryExecutor
    private lateinit var ragLlmComposer: RagLlmComposer
    private lateinit var ragRateLimiter: RagRateLimiter
    private lateinit var ragQueryLogCommand: RagQueryLogCommand
    private lateinit var objectMapper: ObjectMapper
    private lateinit var service: RagService

    private val memberId = 10L
    private val question = "백엔드 공고 추천해줘"

    private val stubQuery = RagQuery(
        intent = RagIntent.DOCUMENT_SEARCH,
        semanticRetrieval = true,
        aggregation = false,
        baseline = false,
        filters = RagFilters(),
        parsedText = "백엔드 Spring Boot"
    )

    private val stubContext = RagContext()

    private val stubAnswer = RagAnswer(
        answer = "관련 공고 3건입니다.",
        intent = RagIntent.DOCUMENT_SEARCH,
        reasoning = "top-3 유사 공고 기반",
        evidences = null,
        sources = null
    )

    @BeforeEach
    fun setUp() {
        ragLlmParser = mockk()
        ragQueryExecutor = mockk()
        ragLlmComposer = mockk()
        ragRateLimiter = mockk()
        ragQueryLogCommand = mockk()
        objectMapper = ObjectMapper()

        service = RagService(
            ragLlmParser, ragQueryExecutor, ragLlmComposer,
            ragRateLimiter, ragQueryLogCommand, objectMapper
        )

        justRun { ragRateLimiter.checkAndIncrement(any(), any()) }
        every { ragLlmParser.parse(question) } returns stubQuery
        every { ragQueryExecutor.execute(stubQuery, memberId) } returns stubContext
        every { ragLlmComposer.compose(question, stubQuery.intent, stubContext) } returns stubAnswer
        justRun { ragQueryLogCommand.save(any()) }
    }

    @Nested
    @DisplayName("query는")
    inner class QueryTest {

        @Test
        @DisplayName("정상 흐름에서 Parser → Executor → Composer 순서로 실행한다")
        fun shouldExecuteInOrder() {
            val answer = service.query(question, memberId, isAdmin = false)

            assertThat(answer.answer).isEqualTo(stubAnswer.answer)
            assertThat(answer.intent).isEqualTo(RagIntent.DOCUMENT_SEARCH)

            verify(ordering = io.mockk.Ordering.ORDERED) {
                ragRateLimiter.checkAndIncrement(memberId, false)
                ragLlmParser.parse(question)
                ragQueryExecutor.execute(stubQuery, memberId)
                ragLlmComposer.compose(question, stubQuery.intent, stubContext)
            }
        }

        @Test
        @DisplayName("정상 흐름에서 QueryLog를 저장한다")
        fun shouldSaveQueryLog() {
            service.query(question, memberId, isAdmin = false)

            verify(exactly = 1) { ragQueryLogCommand.save(any()) }
        }

        @Test
        @DisplayName("QueryLog 저장 실패 시에도 answer를 정상 반환한다")
        fun shouldReturnAnswerEvenWhenLogSaveFails() {
            every { ragQueryLogCommand.save(any()) } throws RuntimeException("DB 장애")

            val answer = service.query(question, memberId, isAdmin = false)

            assertThat(answer.answer).isEqualTo(stubAnswer.answer)
        }

        @Test
        @DisplayName("isAdmin=true이면 RateLimiter에 isAdmin=true로 전달한다")
        fun shouldPassIsAdminToRateLimiter() {
            service.query(question, memberId, isAdmin = true)

            verify(exactly = 1) { ragRateLimiter.checkAndIncrement(memberId, true) }
        }
    }
}