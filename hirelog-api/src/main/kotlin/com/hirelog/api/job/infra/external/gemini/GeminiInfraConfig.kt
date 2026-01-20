package com.hirelog.api.job.infrastructure.external.gemini

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.config.properties.GeminiProperties
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
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
     * Gemini API 호출용 HTTP Client Bean
     *
     * 역할:
     * - Gemini API 전용 WebClient를 GeminiClient에 주입
     *
     * 주의:
     * - WebClient 생성 로직은 GeminiWebClientConfig에 존재한다
     * - 이 메서드는 생성된 WebClient를 "조립"만 한다
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
     * Gemini 응답 파서 Bean
     *
     * 역할:
     * - Gemini API 응답(JSON/Markdown)을
     *   JobSummaryLlmResult로 변환한다
     *
     * 설계 의도:
     * - 파싱 로직을 Client에서 분리
     * - 응답 포맷 변경 시 수정 지점을 한 곳으로 집중
     */
    @Bean
    fun geminiResponseParser(
        objectMapper: ObjectMapper
    ): GeminiResponseParser =
        GeminiResponseParser(objectMapper)

    /**
     * JobSummaryLlm Port 구현체 Bean
     *
     * 역할:
     * - JobSummaryLlm 인터페이스에 대해
     *   Gemini 기반 구현체를 연결한다
     *
     * 핵심:
     * - Application 계층은 JobSummaryLlm만 의존한다
     * - GeminiJobSummaryClient는 이 Config를 통해서만 노출된다
     */
    @Bean
    fun jobSummaryLlm(
        geminiClient: GeminiClient,
        responseParser: GeminiResponseParser
    ): JobSummaryLlm =
        GeminiJobSummaryLlm(
            geminiClient = geminiClient,
            responseParser = responseParser
        )
}
