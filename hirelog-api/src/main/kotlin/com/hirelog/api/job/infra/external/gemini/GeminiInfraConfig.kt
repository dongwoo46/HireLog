package com.hirelog.api.job.infra.external.gemini

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.config.properties.AiProperties
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.infra.external.common.JobSummaryLlmResultAssembler
import com.hirelog.api.job.infra.external.common.LlmResponseParser
import com.hirelog.api.job.infrastructure.external.gemini.GeminiClient
import com.hirelog.api.job.infrastructure.external.gemini.GeminiJobSummaryLlm
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

/**
 * Gemini LLM Infra Configuration
 *
 * 책임:
 * - Gemini 관련 인프라 객체들을 Spring Bean으로 조립한다
 * - JobSummaryLlm Port에 대한 실제 구현체를 결정한다
 * - Circuit Breaker를 통한 장애 격리
 *
 * 설계 원칙:
 * - 외부 시스템(LLM)과의 연결은 Config 계층에서만 수행한다
 * - Application / Domain 계층은 Gemini 구현을 알지 않는다
 * - 어떤 LLM을 사용할지는 이 설정 클래스만 보면 알 수 있다
 * - Circuit Breaker로 외부 API 장애로부터 시스템 보호
 */
@Configuration
@Profile("!loadtest")
class GeminiInfraConfig(
    private val aiProperties: AiProperties
) {

    /**
     * Gemini Circuit Breaker
     */
    @Bean
    fun geminiCircuitBreaker(
        circuitBreakerRegistry: CircuitBreakerRegistry
    ): CircuitBreaker {
        return circuitBreakerRegistry.circuitBreaker("gemini")
    }

    /**
     * Gemini API 전용 Client
     */
    @Bean
    fun geminiClient(
        @Qualifier("geminiWebClient") webClient: WebClient
    ): GeminiClient =
        GeminiClient(
            webClient = webClient,
            properties = aiProperties.gemini
        )

    /**
     * LLM 응답 Raw 파서 (공통)
     */
    @Bean
    fun llmResponseParser(
        objectMapper: ObjectMapper
    ): LlmResponseParser =
        LlmResponseParser(objectMapper)

    /**
     * Raw → Result 변환기 (공통)
     */
    @Bean
    fun jobSummaryLlmResultAssembler(): JobSummaryLlmResultAssembler =
        JobSummaryLlmResultAssembler()

    /**
     * JobSummaryLlm Port 구현체 (Gemini)
     */
    @Bean("geminiJobSummaryLlm")
    fun geminiJobSummaryLlm(
        geminiClient: GeminiClient,
        responseParser: LlmResponseParser,
        assembler: JobSummaryLlmResultAssembler,
        geminiCircuitBreaker: CircuitBreaker,
        meterRegistry: MeterRegistry
    ): JobSummaryLlm  {
        log.info("[Gemini_LLM_CONFIG] GeminiJobSummaryLlm initialized")
        return GeminiJobSummaryLlm(
            geminiClient = geminiClient,
            responseParser = responseParser,
            assembler = assembler,
            circuitBreaker = geminiCircuitBreaker,
            meterRegistry = meterRegistry
        )

    }

}
