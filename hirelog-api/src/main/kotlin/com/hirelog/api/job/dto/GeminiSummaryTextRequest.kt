package com.hirelog.api.job.dto

data class GeminiSummaryTextRequest(
    val companyName: String,
    val position: String,
    val jdText: String
)
