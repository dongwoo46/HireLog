package com.hirelog.api.common.config.properties

data class LlmProviderProperties(
    val apiKey: String,
    val model: String,
    val timeoutMs: Long
)
