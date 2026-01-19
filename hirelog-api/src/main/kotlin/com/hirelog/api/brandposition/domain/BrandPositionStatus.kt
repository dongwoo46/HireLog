package com.hirelog.api.brandposition.domain

enum class BrandPositionStatus {

    /**
     * LLM 자동 생성 / 관리자 검증 대기
     */
    CANDIDATE,

    /**
     * 관리자 승인 완료 + 실제 사용 중
     */
    ACTIVE,

    /**
     * 더 이상 사용하지 않음
     * (과거 데이터 보존 목적)
     */
    INACTIVE
}
