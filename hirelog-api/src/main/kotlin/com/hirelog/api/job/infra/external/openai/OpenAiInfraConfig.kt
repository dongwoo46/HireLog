package com.hirelog.api.job.infra.external.openai

import com.hirelog.api.common.config.properties.AiProperties
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.infra.external.common.LlmResponseParser
import com.hirelog.api.job.infra.external.common.JobSummaryLlmResultAssembler
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * OpenAI LLM Infra Configuration
 *
 * 책임:
 * - OpenAI 관련 인프라 객체들을 Spring Bean으로 조립
 * - Gemini fallback용 LLM 구현체 제공
 */
@Configuration
class OpenAiInfraConfig(
    private val aiProperties: AiProperties
) {

    @Bean
    fun openAiCircuitBreaker(
        circuitBreakerRegistry: CircuitBreakerRegistry
    ): CircuitBreaker {
        return circuitBreakerRegistry.circuitBreaker("openai")
    }

    @Bean
    fun openAiClient(
        @Qualifier("openAiWebClient") webClient: WebClient
    ): OpenAiClient =
        OpenAiClient(
            webClient = webClient,
            properties = aiProperties.openai
        )

    @Bean("openAiJobSummaryLlm")
    fun openAiJobSummaryLlm(
        openAiClient: OpenAiClient,
        responseParser: LlmResponseParser,
        assembler: JobSummaryLlmResultAssembler,
        openAiCircuitBreaker: CircuitBreaker
    ): JobSummaryLlm =
        OpenAiJobSummaryLlm(
            openAiClient = openAiClient,
            responseParser = responseParser,
            assembler = assembler,
            circuitBreaker = openAiCircuitBreaker
        )
}
