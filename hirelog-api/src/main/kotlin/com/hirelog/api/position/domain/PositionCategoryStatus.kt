package com.hirelog.api.position.domain

enum class PositionCategoryStatus {

    /**
     * 정상 사용 중
     *
     * - 신규 Position 생성 시 선택 가능
     * - 검색 / 통계 / 필터 기준으로 사용됨
     */
    ACTIVE,

    /**
     * 더 이상 신규 Position에 사용하지 않음
     *
     * - 기존 Position과의 관계는 유지
     * - 과거 데이터 분석은 가능
     * - 신규 매핑 불가
     */
    INACTIVE
}
