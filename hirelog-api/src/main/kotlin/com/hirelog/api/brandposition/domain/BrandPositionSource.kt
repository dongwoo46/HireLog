package com.hirelog.api.brandposition.domain

enum class BrandPositionSource {

    /**
     * JD 분석 기반 LLM 자동 생성
     */
    LLM,

    /**
     * 관리자가 직접 생성
     */
    ADMIN
}
