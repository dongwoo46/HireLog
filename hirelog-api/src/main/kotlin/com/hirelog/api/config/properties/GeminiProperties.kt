package com.hirelog.api.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties(
    val apiKey: String? = null,
    val model: String = "gemini-1.5-flash",
    val timeoutMs: Long = 10_000
)