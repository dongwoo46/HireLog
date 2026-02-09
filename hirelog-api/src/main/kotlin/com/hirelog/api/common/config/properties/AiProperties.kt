package com.hirelog.api.common.config.properties


import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai")
data class AiProperties(
    val gemini: LlmProviderProperties,
    val openai: LlmProviderProperties
)
