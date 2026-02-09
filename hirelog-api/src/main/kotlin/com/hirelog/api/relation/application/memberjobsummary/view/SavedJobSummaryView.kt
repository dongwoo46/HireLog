package com.hirelog.api.relation.application.memberjobsummary.view

import com.hirelog.api.job.domain.type.CareerType
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import java.time.LocalDateTime

/**
 * SavedJobSummaryView
 *
 * 책임:
 * - 사용자가 저장한 JD 요약 정보를 표현하는 Read Model
 * - MemberJobSummary + JobSummary Join 결과
 *
 * 주의:
 * - 도메인 로직 ❌
 * - 상태 변경 ❌
 */
data class SavedJobSummaryView(
    // === MemberJobSummary 정보 ===
    val memberId: Long,
    val jobSummaryId: Long,
    val saveType: MemberJobSummarySaveType,
    val memo: String?,
    val savedAt: LocalDateTime,

    // === JobSummary 정보 ===
    val brandId: Long,
    val brandName: String,
    val companyId: Long?,
    val companyName: String?,
    val positionId: Long,
    val positionName: String,
    val brandPositionName: String?,
    val careerType: CareerType,
    val careerYears: String?,
    val summaryText: String,
    val techStack: String?
)
