package com.hirelog.api.common.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hirelog.worker")
data class HirelogWorkerProperties(
    val jobSummary: JobSummaryWorkerProperties
)
