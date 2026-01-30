package com.hirelog.api.job.application.summary.command

import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.RecruitmentPeriodType
import java.time.LocalDate

/**
 * JobSummaryGenerateCommand
 *
 * 의미:
 * - JD 요약 생성 유스케이스 실행 명령
 *
 * 특징:
 * - Kafka / Redis / HTTP 등 외부 기술과 무관
 * - Application Service 입력 전용
 */
data class JobSummaryGenerateCommand(

    // Correlation
    val requestId: String,

    // Context
    val brandName: String,
    val positionName: String,

    // Source
    val source: JobSourceType,
    val sourceUrl: String?,

    // Canonical Data
    val canonicalMap: Map<String, List<String>>,

    // Recruitment Info
    val recruitmentPeriodType: RecruitmentPeriodType,
    val openedDate: LocalDate?,
    val closedDate: LocalDate?,

    // Extracted Skills
    val skills: List<String>,

    // Event Time
    val occurredAt: Long
)
