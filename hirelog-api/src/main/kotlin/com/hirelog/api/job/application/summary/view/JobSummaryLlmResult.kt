package com.hirelog.api.job.application.summary.view

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.domain.CareerType

/**
 * JobSummaryLlmResult
 *
 * 🔹 LLM 요약 결과 최종 도메인 모델
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

    // 사용된 LLM Provider (시스템 주입)
    val llmProvider: LlmProvider,

    // 커리어 구분 (UNKNOWN 포함)
    val careerType: CareerType,

    // 요구 경력 연차 (추출 실패 시 null)
    val careerYears: Int?,

    // JD 핵심 요약 (필수)
    val summary: String,

    // 주요 업무 요약 (필수)
    val responsibilities: String,

    // 필수 자격 요건 요약 (필수)
    val requiredQualifications: String,

    // 우대 사항
    val preferredQualifications: String?,

    // 기술 스택 요약 (자유 텍스트)
    val techStack: String?,

    // 채용 절차 요약
    val recruitmentProcess: String?,

    val brandName: String,
    val positionName: String
)
