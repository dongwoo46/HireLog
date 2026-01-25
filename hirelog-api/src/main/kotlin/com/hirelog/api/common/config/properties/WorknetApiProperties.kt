package com.hirelog.api.common.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "worknet.api")
data class WorknetApiProperties(

    val key: String,

    val baseUrl: String,

    val timeoutMs: Long
)
