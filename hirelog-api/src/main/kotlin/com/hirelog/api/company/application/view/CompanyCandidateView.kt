package com.hirelog.api.company.application.view

import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.company.domain.CompanyCandidateStatus
import java.time.LocalDateTime

/**
 * CompanyCandidateView
 *
 * 책임:
 * - CompanyCandidate 운영/관리 조회용 View
 * - 승인/거절 판단에 필요한 정보만 제공
 */
data class CompanyCandidateView(

    /**
     * Candidate 식별자 (승인/거절용)
     */
    val id: Long,

    /**
     * 근거 JD Summary
     */
    val jdSummaryId: Long,

    /**
     * 귀속 Brand
     */
    val brandId: Long,

    /**
     * 추정된 법인명 (사람이 보는 값)
     */
    val candidateName: String,

    /**
     * 추정 출처
     */
    val source: CompanyCandidateSource,

    /**
     * 신뢰도 점수
     */
    val confidenceScore: Double,

    /**
     * 처리 상태
     */
    val status: CompanyCandidateStatus,

    /**
     * 생성 시각 (정렬 / 판단 기준)
     */
    val createdAt: LocalDateTime,
)
