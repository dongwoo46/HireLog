package com.hirelog.api.common.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties(
    val apiKey: String? = null,
    val model: String = "gemini-2.5-flash-lite",
    val timeoutMs: Long = 10_000
)