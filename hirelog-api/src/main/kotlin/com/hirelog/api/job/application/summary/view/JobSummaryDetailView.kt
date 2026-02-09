package com.hirelog.api.job.application.summary.view

import com.hirelog.api.job.domain.type.CareerType
import java.time.LocalDateTime

/**
 * JobSummary 상세 조회 View
 *
 * 용도:
 * - 단건 상세 조회 전용
 * - JobSummary 전체 필드 + Insight(flat) + Reviews 포함
 *
 * 설계:
 * - Projections.fields() 매핑을 위해 전체 필드 기본값 포함
 * - Insight 필드는 flat으로 펼침 (QueryDSL Projection 호환)
 * - reviews는 별도 쿼리 후 copy()로 합산
 */
data class JobSummaryDetailView(

    val summaryId: Long = 0L,
    val snapshotId: Long = 0L,

    // 브랜드
    val brandId: Long = 0L,
    val brandName: String = "",
    val companyId: Long? = null,
    val companyName: String? = null,

    // 포지션
    val positionId: Long = 0L,
    val positionName: String = "",
    val brandPositionId: Long = 0L,
    val brandPositionName: String = "",
    val positionCategoryId: Long = 0L,
    val positionCategoryName: String = "",

    // 경력
    val careerType: CareerType = CareerType.UNKNOWN,
    val careerYears: String? = null,

    // JD 요약
    val summaryText: String = "",
    val responsibilities: String = "",
    val requiredQualifications: String = "",
    val preferredQualifications: String? = null,
    val techStack: String? = null,
    val recruitmentProcess: String? = null,

    // Insight (flat)
    val idealCandidate: String? = null,
    val mustHaveSignals: String? = null,
    val preparationFocus: String? = null,
    val transferableStrengthsAndGapPlan: String? = null,
    val proofPointsAndMetrics: String? = null,
    val storyAngles: String? = null,
    val keyChallenges: String? = null,
    val technicalContext: String? = null,
    val questionsToAsk: String? = null,
    val considerations: String? = null,

    // 원본 URL
    val sourceUrl: String? = null,

    // 리뷰
    val reviews: List<JobSummaryReviewView> = emptyList(),

    // 현재 사용자의 저장 상태
    val memberJobSummaryId: Long? = null,
    val memberSaveType: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
