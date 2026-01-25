package com.hirelog.api.job.domain

enum class RecruitmentPeriodType {
    FIXED,      // 기간 채용
    OPEN_ENDED, // 상시 채용
    UNKNOWN     // 정보 없음 / 파싱 실패
}