package com.hirelog.api.job.application.summary.view

/**
 * JobSummaryLlmRawResult
 *
 * LLM 원본 응답 중 "요약 도메인으로 변환 가능한 최소 단위"
 *
 * - 모든 필드는 nullable
 * - enum / LocalDate 사용 ❌
 * - 정규화 ❌
 */
data class JobSummaryLlmRawResult(

    // === 기본 정보 ===

    val brandName: String?,
    val positionName: String?,

    // 추론된 법인명 후보 (예: "토스" → "(주)비바리퍼블리카")
    val companyCandidate: String?,

    // 커리어 구분 (예: "신입", "경력", "무관")
    val careerType: String?,

    // 경력 연차 원문 (예: "3년 이상", "5~7년")
    val careerYears: String?,

    // === JD 요약 ===

    // JD 전체 요약
    val summary: String?,

    // 주요 업무
    val responsibilities: String?,

    // 필수 자격 요건
    val requiredQualifications: String?,

    // 우대 사항
    val preferredQualifications: String?,

    // 기술 스택 (자유 텍스트)
    val techStack: String?,

    // 채용 절차 (선택)
    val recruitmentProcess: String?,

    // === Insight 필드 ===

    // 이상적인 지원자 프로필 (2~4문장)
    val idealCandidate: String?,

    // 필수 신호/역량 (newline-separated, max 5)
    val mustHaveSignals: String?,

    // 이력서/면접 준비 포인트 (newline-separated, max 5)
    val preparationFocus: String?,

    // 전환 가능 강점 및 보완 계획
    // Format: "[강점]\nitem1\n...\n\n[보완]\nitem1\n..."
    val transferableStrengthsAndGapPlan: String?,

    // 증거 및 지표
    // Format: "[증거]\nitem1\n...\n\n[지표]\nitem1\n..."
    val proofPointsAndMetrics: String?,

    // 스토리 앵글 (newline-separated, 2~4 lines)
    val storyAngles: String?,

    // 핵심 도전과제 (newline-separated, max 5)
    val keyChallenges: String?,

    // 기술적 맥락 (2~4문장)
    val technicalContext: String?,

    // 면접 질문 (newline-separated, max 7)
    val questionsToAsk: String?,

    // 고려사항 (newline-separated, max 5)
    val considerations: String?
)
