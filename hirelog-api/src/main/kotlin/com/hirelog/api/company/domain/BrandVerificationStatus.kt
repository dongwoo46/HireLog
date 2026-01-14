package com.hirelog.api.company.domain

/**
 * 브랜드 검증 상태
 *
 * - VERIFIED: 실제 존재하며 회사와의 관계가 확인된 브랜드
 * - UNVERIFIED: 사용자 입력 기반, 아직 검증되지 않은 브랜드
 * - REJECTED: 잘못된 브랜드 (오타, 테스트 데이터 등)
 */
enum class BrandVerificationStatus {
    VERIFIED,
    UNVERIFIED,
    REJECTED
}
