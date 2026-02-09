package com.hirelog.api.job.application.messaging

import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.domain.type.RecruitmentPeriodType
import java.time.LocalDate

/**
 * Redis Stream Message Contract
 *
 * Python preprocess worker → Spring API
 *
 * 책임:
 * - 외부 메시지 스키마를 타입으로 고정
 *
 * 주의:
 * - Domain Model ❌
 * - Business Logic ❌
 * - Validation ❌
 */
data class JdPreprocessResponseMessage(

    // Metadata
    val type: String,
    val messageVersion: String,
    val createdAt: Long,

    // Correlation
    val requestId: String,

    // Context
    val brandName: String,
    val positionName: String,

    // Source
    val source: JobSourceType,
    val sourceUrl: String?,

    // Canonical result
    val canonicalMap: Map<String, List<String>>,

    // Recruitment info
    val recruitmentPeriodType: RecruitmentPeriodType,
    val openedDate: LocalDate?,
    val closedDate: LocalDate?,

    // Extracted skills
    val skills: List<String>
)
