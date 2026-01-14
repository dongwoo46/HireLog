package com.hirelog.api.company.domain

enum class CompanyVerificationStatus {
    VERIFIED,     // 공공데이터 등으로 검증됨
    UNVERIFIED,   // 사용자 입력 / 추론 기반
    REJECTED      // 잘못된 회사
}
