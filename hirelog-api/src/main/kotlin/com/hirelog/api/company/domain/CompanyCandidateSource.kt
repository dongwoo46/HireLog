package com.hirelog.api.company.domain

enum class CompanyCandidateSource {
    /**
     * LLM 추론 결과
     */
    LLM,

    /**
     * 규칙 기반 추출
     */
    RULE,

    /**
     * 관리자 수동 입력
     */
    MANUAL
}
