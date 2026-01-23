package com.hirelog.api.job.application.summary.view

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.domain.CareerType
import com.hirelog.api.job.domain.EmploymentType
import com.hirelog.api.job.domain.WorkStyle
import java.time.LocalDate

/**
 * LLM 요약 결과 공통 모델
 *
 * 역할:
 * - 채용 공고(JD)를 LLM이 해석한 구조화 결과 표현
 * - 도메인 판단, 저장, 통계의 기준 데이터
 */
data class JobSummaryLlmResult(

    // 커리어 구분 (신입 / 경력 / 무관 등)
    val careerType: CareerType,

    // 요구 경력 연차 (명시되지 않으면 null)
    val careerYears: Int?,

    // JD 전체를 한 문단으로 요약한 핵심 설명
    val summary: String,

    // 주요 업무 내용 요약
    val responsibilities: String,

    // 필수 자격 요건 요약
    val requiredQualifications: String,

    // 우대 사항 요약
    val preferredQualifications: String?,

    // 기술 스택 요약
    val techStack: String?,

    // 채용 절차 요약
    val recruitmentProcess: String?,

    // 고용 형태 (정규직 / 계약직 / 인턴 등)
    val employmentType: EmploymentType?,

    // 근무 지역 정보 (정규화 전 원문 기반)
    val workLocation: String?,

    // 근무 방식 (상주 / 재택 / 혼합)
    val workStyle: WorkStyle?,

    // 연봉 또는 보상 조건 요약
    val compensation: String?,

    // 복지 및 혜택 요약
    val benefits: String?,

    // 공고 시작일 (추출 가능할 경우)
    val openingDate: LocalDate?,

    // 공고 마감일 (추출 가능할 경우)
    val closingDate: LocalDate?,

    // 회사 또는 팀에 대한 간단한 소개
    val companyDescription: String?,

    // 요약에 사용된 LLM 제공자 식별자
    val llmProvider: LlmProvider
)
