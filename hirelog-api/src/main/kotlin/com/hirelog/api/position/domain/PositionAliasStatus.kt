package com.hirelog.api.position.domain

enum class PositionAliasStatus {

    /**
     * 신규 생성됨
     * - 자동 생성
     * - 아직 검증되지 않음
     */
    PENDING,

    /**
     * 관리자 검증 완료
     * - 매핑에 사용 가능
     */
    ACTIVE,

    /**
     * 더 이상 사용하지 않음
     * - 잘못된 매핑
     * - 폐기된 표현
     */
    INACTIVE
}
