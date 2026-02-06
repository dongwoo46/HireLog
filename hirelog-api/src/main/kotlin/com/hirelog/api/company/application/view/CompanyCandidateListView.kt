package com.hirelog.api.company.application.view

import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.company.domain.CompanyCandidateStatus
import java.time.LocalDateTime

/**
 * CompanyCandidateListView
 *
 * 책임:
 * - CompanyCandidate 목록 조회용 Projection
 * - 운영/검토 화면 최적화
 */
data class CompanyCandidateListView(
    val id: Long,
    val candidateName: String,
    val source: CompanyCandidateSource,
    val confidenceScore: Double,
    val status: CompanyCandidateStatus,
    val createdAt: LocalDateTime
)
