package com.hirelog.api.company.domain

enum class CompanySource {
    FAIR_TRADE_COMMISSION,   // 공정위
    NATIONAL_TAX_SERVICE,    // 국세청
    USER,                    // 사용자 입력
    ADMIN,                   // 관리자 수동 등록
    CRAWLED                  // 크롤링 추론
}
