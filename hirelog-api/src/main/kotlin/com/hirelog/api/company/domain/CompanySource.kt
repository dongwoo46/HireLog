package com.hirelog.api.company.domain

enum class CompanySource {
    FAIR_TRADE_COMMISSION,   // 공정위
    NATIONAL_TAX_SERVICE,    // 국세청
    ADMIN,                   // 관리자 수동 등록
    LLM                  // LLM
}
