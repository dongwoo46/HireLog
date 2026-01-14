package com.hirelog.api.job.dto

import com.hirelog.api.job.domain.CareerType

data class JobSummaryResult(
    val brandName: String,
    val position: String,

    val careerType: CareerType,
    val careerYears: Int?,

    val summary: String,
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,
    val techStack: String?,
    val recruitmentProcess: String?
)
