package com.hirelog.api.company.application.view

import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.company.domain.CompanyCandidateStatus
import java.time.LocalDateTime

/**
 * CompanyCandidateDetailView
 *
 * 책임:
 * - CompanyCandidate 단건 상세 조회 Projection
 */
data class CompanyCandidateDetailView(
    val id: Long,
    val candidateName: String,
    val normalizedName: String,
    val source: CompanyCandidateSource,
    val confidenceScore: Double,
    val status: CompanyCandidateStatus,

    val brand: BrandSimpleView?,
    val jdSummaryId: Long,

    val createdAt: LocalDateTime
)

/**
 * BrandSimpleView
 *
 * 책임:
 * - CompanyCandidate 상세 화면에서 사용하는 Brand 최소 정보
 */
data class BrandSimpleView(
    val id: Long,
    val name: String,
    val isActive: Boolean
)