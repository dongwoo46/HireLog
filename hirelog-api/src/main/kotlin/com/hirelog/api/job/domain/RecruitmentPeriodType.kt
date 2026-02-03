package com.hirelog.api.job.domain

enum class RecruitmentPeriodType {
    FIXED,        // 명확한 날짜 범위
    OPEN_ENDED,   // 종료일 없는 채용
    UNKNOWN
}

/**
 * Python 전처리 결과 → RecruitmentPeriodType 변환
 */
object RecruitmentPeriodTypeMapper {

    fun fromRaw(raw: String?): RecruitmentPeriodType {
        if (raw == null) return RecruitmentPeriodType.UNKNOWN

        return when (raw.uppercase()) {
            "FIXED" -> RecruitmentPeriodType.FIXED
            "ALWAYS", "OPEN", "OPEN_ENDED" -> RecruitmentPeriodType.OPEN_ENDED
            else -> RecruitmentPeriodType.UNKNOWN
        }
    }
}
