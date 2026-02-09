package com.hirelog.api.job.infra.external.gemini

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.infrastructure.external.gemini.GeminiClient
import com.hirelog.api.job.infrastructure.external.gemini.GeminiJobSummaryLlm
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration
import java.util.concurrent.CompletableFuture

@DisplayName("GeminiJobSummaryLlm Circuit Breaker 테스트")
class GeminiJobSummaryLlmTest {

    private lateinit var geminiClient: GeminiClient
    private lateinit var responseParser: GeminiResponseParser
    private lateinit var assembler: JobSummaryLlmResultAssembler
    private lateinit var circuitBreaker: CircuitBreaker
    private lateinit var llm: GeminiJobSummaryLlm

    @BeforeEach
    fun setup() {
        geminiClient = mockk()
        responseParser = mockk()
        assembler = mockk()

        // Circuit Breaker 설정 (테스트용)
        val config = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(5)
            .failureRateThreshold(50f)
            .minimumNumberOfCalls(3)
            .waitDurationInOpenState(Duration.ofSeconds(2))
            .permittedNumberOfCallsInHalfOpenState(2)
            .recordExceptions(
                WebClientResponseException.ServiceUnavailable::class.java,
                GeminiCallException::class.java
            )
            .build()

        val registry = CircuitBreakerRegistry.of(config)
        circuitBreaker = registry.circuitBreaker("gemini-test")

        llm = GeminiJobSummaryLlm(geminiClient, responseParser, assembler, circuitBreaker)

        // 각 테스트 전 Circuit Breaker 리셋
        circuitBreaker.reset()
    }

    @Nested
    @DisplayName("Circuit이 CLOSED 상태일 때")
    inner class WhenCircuitClosed {

        @Test
        @DisplayName("정상 호출이 성공한다")
        fun successfulCall() {
            // given
            val mockResult = mockk<JobSummaryLlmResult>()
            every { geminiClient.generateContentAsync(any()) } returns
                    CompletableFuture.completedFuture("raw response")
            every { responseParser.parseRawJobSummary(any()) } returns mockk()
            every { assembler.assemble(any(), LlmProvider.GEMINI) } returns mockResult

            // when
            val result = llm.summarizeJobDescriptionAsync(
                "TestBrand", "TestPosition",
                emptyList(), emptyList(), emptyMap()
            ).get()

            // then
            assertEquals(mockResult, result)
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
        }
    }

    @Nested
    @DisplayName("연속된 실패로 Circuit이 OPEN될 때")
    inner class WhenCircuitOpens {

        @Test
        @DisplayName("실패 threshold 도달 후 Circuit이 OPEN된다")
        fun opensAfterThreshold() {
            // given
            every { geminiClient.generateContentAsync(any()) } returns
                    CompletableFuture.failedFuture(
                        WebClientResponseException(
                            "Service Unavailable",
                            503,
                            "Service Unavailable",
                            HttpHeaders.EMPTY,
                            ByteArray(0),
                            null
                        )
                    )

            // when - 최소 3번 실패 (50% 이상 실패율 달성)
            repeat(3) {
                val exception = assertThrows<ExecutionException> {
                    llm.summarizeJobDescriptionAsync(
                        "TestBrand", "TestPosition",
                        emptyList(), emptyList(), emptyMap()
                    ).get()
                }
                assertTrue(exception.cause is GeminiCallException)
            }

            // then
            assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
            verify(exactly = 3) { geminiClient.generateContentAsync(any()) }  // 3번만!
        }

        @Test
        @DisplayName("Circuit OPEN 상태에서는 즉시 실패한다")
        fun failsImmediatelyWhenOpen() {
            // given - Circuit을 OPEN으로 만듦
            every { geminiClient.generateContentAsync(any()) } returns
                    CompletableFuture.failedFuture(RuntimeException("Fail"))

            repeat(3) {
                runCatching {
                    llm.summarizeJobDescriptionAsync(
                        "TestBrand", "TestPosition",
                        emptyList(), emptyList(), emptyMap()
                    ).get()
                }
            }

            assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)

            // when - OPEN 상태에서 추가 호출
            assertThrows<GeminiCallException> {
                llm.summarizeJobDescriptionAsync(
                    "TestBrand", "TestPosition",
                    emptyList(), emptyList(), emptyMap()
                ).get()
            }

            // then - 더 이상 호출 안됨
            verify(exactly = 3) { geminiClient.generateContentAsync(any()) }
        }
    }

    @Nested
    @DisplayName("Circuit이 HALF_OPEN으로 전환될 때")
    inner class WhenCircuitHalfOpen {

        @Test
        @DisplayName("waitDuration 이후 HALF_OPEN 상태가 된다")
        fun transitionsToHalfOpen() {
            // given - OPEN으로 만듦
            every { geminiClient.generateContentAsync(any()) } returns
                    CompletableFuture.failedFuture(RuntimeException("Fail"))

            repeat(3) {
                runCatching {
                    llm.summarizeJobDescriptionAsync(
                        "TestBrand", "TestPosition",
                        emptyList(), emptyList(), emptyMap()
                    ).get()
                }
            }

            assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)

            // when - 2초 대기
            Thread.sleep(2100)

            val mockResult = mockk<JobSummaryLlmResult>()
            every { geminiClient.generateContentAsync(any()) } returns
                    CompletableFuture.completedFuture("success")
            every { responseParser.parseRawJobSummary(any()) } returns mockk()
            every { assembler.assemble(any(), LlmProvider.GEMINI) } returns mockResult

            llm.summarizeJobDescriptionAsync(
                "TestBrand", "TestPosition",
                emptyList(), emptyList(), emptyMap()
            ).get()

            // then
            assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.state)
        }

        @Test
        @DisplayName("HALF_OPEN에서 성공하면 CLOSED로 복구된다")
        fun recoversToClosedOnSuccess() {
            // given - OPEN 만들기
            every { geminiClient.generateContentAsync(any()) } returns
                    CompletableFuture.failedFuture(RuntimeException("Fail"))

            repeat(3) {
                runCatching {
                    llm.summarizeJobDescriptionAsync(
                        "TestBrand", "TestPosition",
                        emptyList(), emptyList(), emptyMap()
                    ).get()
                }
            }

            Thread.sleep(2100)

            // when - 성공 응답
            val mockResult = mockk<JobSummaryLlmResult>()
            every { geminiClient.generateContentAsync(any()) } returns
                    CompletableFuture.completedFuture("success")
            every { responseParser.parseRawJobSummary(any()) } returns mockk()
            every { assembler.assemble(any(), LlmProvider.GEMINI) } returns mockResult

            repeat(2) {
                llm.summarizeJobDescriptionAsync(
                    "TestBrand", "TestPosition",
                    emptyList(), emptyList(), emptyMap()
                ).get()
            }

            // then
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
        }
    }
}