package com.hirelog.api.job.service

import com.hirelog.api.job.domain.CareerType

data class GeminiSummary(
    val careerType: CareerType,
    val careerYears: Int?,

    val summary: String,
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,
    val techStack: String?,
    val recruitmentProcess: String?
)
