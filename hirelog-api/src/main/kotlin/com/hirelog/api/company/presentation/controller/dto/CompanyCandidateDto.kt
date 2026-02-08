package com.hirelog.api.company.presentation.controller.dto


import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.company.domain.CompanyCandidateStatus

/**
 * CompanyCandidateSearchReq
 *
 * 책임:
 * - CompanyCandidate 검색 조건 전달
 */
data class CompanyCandidateSearchReq(

    val candidateName: String? = null,

    val source: CompanyCandidateSource? = null,

    val status: CompanyCandidateStatus? = null,

    val minConfidenceScore: Double? = null,

    val maxConfidenceScore: Double? = null
)
