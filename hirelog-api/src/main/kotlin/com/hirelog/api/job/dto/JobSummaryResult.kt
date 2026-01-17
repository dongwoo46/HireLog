package com.hirelog.api.job.dto

import com.hirelog.api.job.domain.CareerType
import com.hirelog.api.job.domain.JobSummary

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
) {
    companion object {
        fun from(entity: JobSummary): JobSummaryResult =
            JobSummaryResult(
                brandName = entity.brandName,
                position = entity.positionName,

                careerType = entity.careerType,
                careerYears = entity.careerYears,

                summary = entity.summaryText,
                responsibilities = entity.responsibilities,
                requiredQualifications = entity.requiredQualifications,
                preferredQualifications = entity.preferredQualifications,
                techStack = entity.techStack,
                recruitmentProcess = entity.recruitmentProcess
            )
    }
}
