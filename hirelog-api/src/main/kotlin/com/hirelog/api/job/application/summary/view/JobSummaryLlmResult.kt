package com.hirelog.api.job.application.summary.view

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.domain.CareerType

/**
 * JobSummaryLlmResult
 *
 * LLM 요약 결과 최종 도메인 모델
 *
 * 책임:
 * - 시스템이 신뢰하는 JD 요약 결과 표현
 * - JobSummary 엔티티 생성의 직접 입력값
 *
 * 생성 규칙:
 * - JobSummaryLlmRawResult → Assembler를 통해서만 생성
 * - Jackson 직접 파싱 ❌
 */
data class JobSummaryLlmResult(

    // === 메타 정보 ===

    val llmProvider: LlmProvider,

    // === 기본 정보 ===

    val brandName: String,
    val positionName: String,
    val brandPositionName: String?,

    // 추론된 법인명 후보 (예: "토스" → "(주)비바리퍼블리카")
    // CompanyCandidate 생성에 사용
    val companyCandidate: String?,

    val careerType: CareerType,

    // 경력 연차 원문 (예: "3년 이상", "5~7년")
    // null: 미기재 또는 신입/무관
    val careerYears: String?,

    // === JD 요약 ===

    val summary: String,
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,
    val techStack: String?,
    val recruitmentProcess: String?,

    // === Insight ===

    val insight: JobSummaryInsightResult
)

/**
 * LLM Insight 결과
 *
 * 모든 필드는 nullable (LLM이 추출 실패할 수 있음)
 */
data class JobSummaryInsightResult(
    val idealCandidate: String?,
    val mustHaveSignals: String?,
    val preparationFocus: String?,
    val transferableStrengthsAndGapPlan: String?,
    val proofPointsAndMetrics: String?,
    val storyAngles: String?,
    val keyChallenges: String?,
    val technicalContext: String?,
    val questionsToAsk: String?,
    val considerations: String?
)
