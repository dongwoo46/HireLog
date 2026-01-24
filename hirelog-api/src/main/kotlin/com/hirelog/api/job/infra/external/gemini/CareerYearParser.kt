package com.hirelog.api.job.infra.external.gemini

object CareerYearParser {

    /**
     * 경력 연차 문자열을 최소 요구 연차(Int)로 변환
     *
     * 규칙:
     * - 숫자가 명확히 존재하면 가장 작은 값 사용
     * - 신입 / 무관 / 파싱 실패 → null
     */
    fun parse(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null

        // 숫자 추출 (예: "3년 이상", "3~5년", "10년+")
        val numbers = Regex("\\d+")
            .findAll(raw)
            .map { it.value.toInt() }
            .toList()

        return numbers.minOrNull()
    }
}
