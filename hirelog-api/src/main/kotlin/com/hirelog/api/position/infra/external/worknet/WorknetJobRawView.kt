package com.hirelog.api.position.infra.external.worknet.dto

/**
 * Worknet 직업 원본 데이터 View
 *
 * 책임:
 * - Worknet OpenAPI에서 내려주는 필드 그대로 표현
 * - 디버그 / 탐색 / 초기 적재 용도
 */
data class WorknetJobRawView(
    val jobCode: String,          // K000000847
    val jobName: String,          // 기업고위임원
    val jobCategoryCode: String?, // 011
    val jobCategoryName: String?  // 의회의원·고위공무원 및 기업 고위임원
)
