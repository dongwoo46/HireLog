package com.hirelog.api.common.config.infra

import com.hirelog.api.common.config.properties.WorknetApiProperties
import com.hirelog.api.config.infra.webclient.WebClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
    private val worknetProps: WorknetApiProperties
) {

    /**
     * Gemini LLM 전용 WebClient
     */
    @Bean("geminiWebClient")
    fun geminiWebClient(): WebClient =
        WebClientFactory.create(
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            poolName = "gemini-pool",
            maxConnections = 50,
            responseTimeoutSec = 30,
            maxInMemorySizeMb = 2,
            userAgent = "HireLog-Gemini/1.0"
        )

    /**
     * Worknet OPEN API 전용 WebClient
     *
     * 설정 파일에 없는 값들은
     * - 현재 서비스 특성 기준으로 코드 상수로 고정
     */
    @Bean("worknetWebClient")
    fun worknetWebClient(): WebClient =
        WebClientFactory.create(
            baseUrl = worknetProps.baseUrl,
            poolName = "worknet-api-pool",
            // 공공 API 특성상 과도한 병렬 요청 방지
            maxConnections = 10,
            // yml의 timeout-ms 사용 (ms → sec 변환)
            responseTimeoutSec = worknetProps.timeoutMs / 1000,
            // 직업정보 JSON 크기 고려한 안전한 기본값
            maxInMemorySizeMb = 5,
            userAgent = "HireLog-Worknet/1.0"
        )

    @Bean("openAiWebClient")
    fun openAiWebClient(): WebClient =
        WebClientFactory.create(
            baseUrl = "https://api.openai.com/v1",
            poolName = "openai-pool",
            maxConnections = 50,
            responseTimeoutSec = 30,
            maxInMemorySizeMb = 4, // GPT 응답 크기 고려
            userAgent = "HireLog-OpenAI/1.0"
        )
}
