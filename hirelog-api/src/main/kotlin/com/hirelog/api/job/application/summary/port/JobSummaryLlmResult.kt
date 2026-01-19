package com.hirelog.api.job.application.summary.port

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.domain.CareerType

/**
 * LLM 요약 결과 공통 모델
 *
 * 책임:
 * - LLM 요약 결과의 표준 표현
 * - 요약 내용 + 메타데이터 포함
 *
 * 주의:
 * - 특정 LLM 전용 필드 포함 금지
 * - application / domain에서 바로 사용 가능
 */
data class JobSummaryLlmResult(

     // 커리어 타입 (신입 / 경력 등)
    val careerType: CareerType,

     // 요구 경력 연차
    val careerYears: Int?,

    /**
     * JD 요약 본문
     */
    val summary: String,

    /**
     * 주요 업무
     */
    val responsibilities: String,

    /**
     * 필수 자격 요건
     */
    val requiredQualifications: String,

    /**
     * 우대 사항
     */
    val preferredQualifications: String?,

    /**
     * 기술 스택 요약
     */
    val techStack: String?,

    /**
     * 채용 절차
     */
    val recruitmentProcess: String?,

)
