package com.hirelog.api.common.domain

enum class VerificationStatus {

    /**
     * 공공 데이터 등으로 신뢰 가능한 검증 완료 상태
     */
    VERIFIED,

    /**
     * 아직 검증되지 않음
     * (사용자 입력 / 자동 수집 / 추론 기반)
     */
    UNVERIFIED,

    /**
     * 잘못된 엔티티로 판정됨
     * (회사/브랜드 아님)
     */
    REJECTED
}
