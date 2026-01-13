package com.hirelog.api.job.dto

data class JobSummaryResult(
    val companyName: String,
    val position: String,
    val summary: String,
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,
    val techStack: String?,
    val recruitmentProcess: String?
)
