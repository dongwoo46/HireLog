package com.hirelog.api.common.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "embedding.server")
data class EmbeddingServerProperties(
    val url: String,
    val timeoutMs: Long = 10_000
)
