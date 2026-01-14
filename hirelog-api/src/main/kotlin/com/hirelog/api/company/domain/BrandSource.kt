package com.hirelog.api.company.domain

/**
 * 브랜드 정보 출처
 */
enum class BrandSource {
    USER,           // 사용자 입력
    ADMIN,          // 관리자 수동 등록
    OFFICIAL,       // 공식 사이트 / 채용 페이지
    EXTERNAL_DATA   // 외부 데이터 소스 (크롤링, API 등)
}
