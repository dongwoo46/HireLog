package com.hirelog.api.job.presentation.controller.dto.response

import com.hirelog.api.job.domain.type.CareerType
import com.hirelog.api.job.domain.model.JobSummary

data class JobSummaryRes(
    val brandName: String,
    val position: String,

    val careerType: CareerType,
    val careerYears: String?,

    val summary: String,
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,
    val techStack: String?,
    val recruitmentProcess: String?
) {
    companion object {
        fun from(entity: JobSummary): JobSummaryRes =
            JobSummaryRes(
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
