package com.hirelog.api.job.infra.external.gemini

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.infrastructure.external.gemini.GeminiClient
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

@DisplayName("GeminiRagParserAdapter 테스트")
class GeminiRagParserAdapterTest {

    private lateinit var adapter: GeminiRagParserAdapter
    private lateinit var geminiClient: GeminiClient

    // ObjectMapper는 인-프로세스 의존성 → 실제 객체 사용
    private val objectMapper = ObjectMapper().registerKotlinModule()

    @BeforeEach
    fun setUp() {
        geminiClient = mockk()
        adapter = GeminiRagParserAdapter(geminiClient, objectMapper)
    }

    @Nested
    @DisplayName("Gemini가 유효한 JSON을 반환하면")
    inner class ValidJsonTest {

        @Test
        @DisplayName("DOCUMENT_SEARCH intent와 필터를 올바르게 파싱한다")
        fun shouldParseDocumentSearchQueryCorrectly() {
            // Arrange
            val json = """
                {
                  "intent": "DOCUMENT_SEARCH",
                  "semanticRetrieval": true,
                  "aggregation": false,
                  "baseline": false,
                  "parsedText": "Kafka 백엔드 개발자",
                  "filters": {
                    "saveType": null,
                    "stage": null,
                    "stageResult": null,
                    "careerType": "EXPERIENCED",
                    "companyDomain": "FINTECH",
                    "techStacks": ["Kafka", "Spring Boot"],
                    "brandName": null,
                    "dateRangeFrom": null,
                    "dateRangeTo": null
                  }
                }
            """.trimIndent()
            every { geminiClient.generateContentWithSystemAsync(any(), any()) } returns
                CompletableFuture.completedFuture(json)

            // Act
            val result = adapter.parse("Kafka 쓰는 핀테크 공고 찾아줘")

            // Assert
            assertThat(result.intent).isEqualTo(RagIntent.DOCUMENT_SEARCH)
            assertThat(result.semanticRetrieval).isTrue()
            assertThat(result.aggregation).isFalse()
            assertThat(result.parsedText).isEqualTo("Kafka 백엔드 개발자")
            assertThat(result.filters.careerType).isEqualTo("EXPERIENCED")
            assertThat(result.filters.companyDomain).isEqualTo("FINTECH")
            assertThat(result.filters.techStacks).containsExactly("Kafka", "Spring Boot")
        }

        @Test
        @DisplayName("STATISTICS intent에 cohort 필터를 올바르게 파싱한다")
        fun shouldParseStatisticsQueryWithCohortFilters() {
            // Arrange
            val json = """
                {
                  "intent": "STATISTICS",
                  "semanticRetrieval": false,
                  "aggregation": true,
                  "baseline": true,
                  "parsedText": "내가 저장한 공고 기술스택 통계",
                  "filters": {
                    "saveType": "SAVED",
                    "stage": null,
                    "stageResult": null,
                    "careerType": null,
                    "companyDomain": null,
                    "techStacks": null,
                    "brandName": null,
                    "dateRangeFrom": null,
                    "dateRangeTo": null
                  }
                }
            """.trimIndent()
            every { geminiClient.generateContentWithSystemAsync(any(), any()) } returns
                CompletableFuture.completedFuture(json)

            // Act
            val result = adapter.parse("내가 저장한 공고 기술스택 전체 대비 통계 알려줘")

            // Assert
            assertThat(result.intent).isEqualTo(RagIntent.STATISTICS)
            assertThat(result.aggregation).isTrue()
            assertThat(result.baseline).isTrue()
            assertThat(result.filters.saveType).isNotNull()
        }

        @Test
        @DisplayName("알 수 없는 intent 문자열이면 DOCUMENT_SEARCH로 fallback한다")
        fun shouldFallbackToDocumentSearchWhenIntentIsUnknown() {
            // Arrange
            // semanticRetrieval=null 이면 intent 기반으로 재계산 (DOCUMENT_SEARCH → true)
            val json = """
                {
                  "intent": "UNKNOWN_INTENT",
                  "semanticRetrieval": null,
                  "aggregation": false,
                  "baseline": false,
                  "parsedText": "질문",
                  "filters": {
                    "saveType": null, "stage": null, "stageResult": null,
                    "careerType": null, "companyDomain": null, "techStacks": null,
                    "brandName": null, "dateRangeFrom": null, "dateRangeTo": null
                  }
                }
            """.trimIndent()
            every { geminiClient.generateContentWithSystemAsync(any(), any()) } returns
                CompletableFuture.completedFuture(json)

            // Act
            val result = adapter.parse("질문")

            // Assert — intent 파싱 실패 → DOCUMENT_SEARCH fallback, semanticRetrieval 재계산 → true
            assertThat(result.intent).isEqualTo(RagIntent.DOCUMENT_SEARCH)
            assertThat(result.semanticRetrieval).isTrue()
        }

        @Test
        @DisplayName("Gemini가 ```json 코드블록으로 감싸도 정상 파싱한다")
        fun shouldParseJsonWrappedInCodeBlock() {
            // Arrange
            val jsonWithCodeBlock = """
                ```json
                {
                  "intent": "SUMMARY",
                  "semanticRetrieval": true,
                  "aggregation": false,
                  "baseline": false,
                  "parsedText": "공고 요약",
                  "filters": {
                    "saveType": null, "stage": null, "stageResult": null,
                    "careerType": null, "companyDomain": null, "techStacks": null,
                    "brandName": null, "dateRangeFrom": null, "dateRangeTo": null
                  }
                }
                ```
            """.trimIndent()
            every { geminiClient.generateContentWithSystemAsync(any(), any()) } returns
                CompletableFuture.completedFuture(jsonWithCodeBlock)

            // Act
            val result = adapter.parse("공고 요약해줘")

            // Assert
            assertThat(result.intent).isEqualTo(RagIntent.SUMMARY)
        }
    }

    @Nested
    @DisplayName("Gemini 호출/파싱이 실패하면")
    inner class FallbackTest {

        @Test
        @DisplayName("JSON 파싱 오류 시 DOCUMENT_SEARCH fallback을 반환한다")
        fun shouldReturnDocumentSearchFallbackOnMalformedJson() {
            // Arrange
            every { geminiClient.generateContentWithSystemAsync(any(), any()) } returns
                CompletableFuture.completedFuture("not a valid json {{ }}")

            // Act
            val result = adapter.parse("어떤 질문이든")

            // Assert
            assertThat(result.intent).isEqualTo(RagIntent.DOCUMENT_SEARCH)
            assertThat(result.semanticRetrieval).isTrue()
            assertThat(result.aggregation).isFalse()
            assertThat(result.parsedText).isEqualTo("어떤 질문이든")
        }

        @Test
        @DisplayName("GeminiClient 예외 발생 시 DOCUMENT_SEARCH fallback을 반환하고 예외를 전파하지 않는다")
        fun shouldReturnDocumentSearchFallbackWhenGeminiClientThrows() {
            // Arrange
            every { geminiClient.generateContentWithSystemAsync(any(), any()) } throws
                RuntimeException("Gemini API 타임아웃")

            // Act — 예외가 전파되면 안 됨
            val result = adapter.parse("질문 내용")

            // Assert
            assertThat(result.intent).isEqualTo(RagIntent.DOCUMENT_SEARCH)
            assertThat(result.parsedText).isEqualTo("질문 내용")
        }

        @Test
        @DisplayName("CompletableFuture 실패 시 DOCUMENT_SEARCH fallback을 반환한다")
        fun shouldReturnDocumentSearchFallbackOnFutureFail() {
            // Arrange
            val failedFuture = CompletableFuture<String>().also {
                it.completeExceptionally(RuntimeException("서버 오류"))
            }
            every { geminiClient.generateContentWithSystemAsync(any(), any()) } returns failedFuture

            // Act
            val result = adapter.parse("질문")

            // Assert
            assertThat(result.intent).isEqualTo(RagIntent.DOCUMENT_SEARCH)
        }
    }
}
