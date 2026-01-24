package com.hirelog.api.job.application.intake.model

import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.RecruitmentPeriodType
import java.time.LocalDate

/**
 * JD Intake 내부 입력 모델
 *
 * 책임:
 * - 전처리 결과 메시지에서
 *   중복/유효성 판단에 필요한 최소 정보만 보유
 *
 * 주의:
 * - 외부 계약 DTO 아님
 * - Domain Model 아님
 */
data class JdIntakeInput(
    val requiredTexts: List<String>,
    val responsibilityTexts: List<String>,
    val preferredTexts: List<String>,
    val openedDate: LocalDate?,
    val closedDate: LocalDate?,
    val recruitmentType: RecruitmentPeriodType,
    val source: JobSourceType,
    val sourceUrl: String?,
    val skills: List<String>?,
    val process: List<String>?
)
