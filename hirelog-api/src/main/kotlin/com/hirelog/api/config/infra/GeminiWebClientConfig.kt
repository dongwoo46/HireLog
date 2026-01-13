package com.hirelog.api.config.infra

import com.hirelog.api.config.infra.webclient.WebClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class GeminiWebClientConfig {

    @Bean("geminiWebClient")
    fun geminiWebClient(): WebClient =
        WebClientFactory.create(
            baseUrl = "https://generativelanguage.googleapis.com/v1",
            poolName = "gemini-pool",
            maxConnections = 50,
            responseTimeoutSec = 30,
            maxInMemorySizeMb = 2,
            userAgent = "HireLog-Gemini/1.0"
        )
}
