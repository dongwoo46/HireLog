package com.hirelog.api.job.infrastructure.external.gemini

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.config.properties.GeminiProperties
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.infra.external.gemini.JobSummaryLlmResultAssembler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Gemini LLM Infra Configuration
 *
 * 책임:
 * - Gemini 관련 인프라 객체들을 Spring Bean으로 조립한다
 * - JobSummaryLlm Port에 대한 실제 구현체를 결정한다
 *
 * 설계 원칙:
 * - 외부 시스템(LLM)과의 연결은 Config 계층에서만 수행한다
 * - Application / Domain 계층은 Gemini 구현을 알지 않는다
 * - 어떤 LLM을 사용할지는 이 설정 클래스만 보면 알 수 있다
 */
@Configuration
class GeminiInfraConfig(
    private val geminiProperties: GeminiProperties
) {

    /**
     * Gemini API 전용 Client
     */
    @Bean
    fun geminiClient(
        @Qualifier("geminiWebClient") webClient: WebClient
    ): GeminiClient =
        GeminiClient(
            webClient = webClient,
            geminiProperties = geminiProperties
        )

    /**
     * Gemini 응답 Raw 파서
     *
     * 책임:
     * - Markdown 제거
     * - JSON → JobSummaryLlmRawResult
     */
    @Bean
    fun geminiResponseParser(
        objectMapper: ObjectMapper
    ): GeminiResponseParser =
        GeminiResponseParser(objectMapper)

    /**
     * Raw → Result 변환기
     *
     * 책임:
     * - enum 변환
     * - 날짜 파싱
     * - 필수 필드 검증
     * - LLM Provider 주입
     */
    @Bean
    fun jobSummaryLlmResultAssembler(): JobSummaryLlmResultAssembler =
        JobSummaryLlmResultAssembler()

    /**
     * JobSummaryLlm Port 구현체
     *
     * 🔥 핵심:
     * - Application 계층은 이 Bean만 의존
     * - Gemini 구현 상세는 여기서 완전히 숨김
     */
    @Bean
    fun jobSummaryLlm(
        geminiClient: GeminiClient,
        responseParser: GeminiResponseParser,
        assembler: JobSummaryLlmResultAssembler
    ): JobSummaryLlm =
        GeminiJobSummaryLlm(
            geminiClient = geminiClient,
            responseParser = responseParser,
            assembler = assembler
        )
}
