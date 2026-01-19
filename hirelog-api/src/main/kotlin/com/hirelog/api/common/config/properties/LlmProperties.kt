package com.hirelog.api.common.config.properties

import com.hirelog.api.common.domain.LlmProvider
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "llm")
data class LlmProperties(
    val provider: LlmProvider,
    val model: String
)
