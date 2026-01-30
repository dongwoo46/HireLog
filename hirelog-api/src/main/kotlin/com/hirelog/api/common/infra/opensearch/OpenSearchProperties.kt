package com.hirelog.api.common.infra.opensearch

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opensearch")
data class OpenSearchProperties(
    val host: String,
    val port: Int,
    val scheme: String = "http",
    val username: String = "",
    val password: String = "",
)
