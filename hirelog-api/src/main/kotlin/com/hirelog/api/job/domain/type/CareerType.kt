package com.hirelog.api.job.domain.type


/**
 * 커리어 타입
 *
 * 설계 원칙:
 * - LLM 응답은 신뢰하지 않는다
 * - from()을 통해서만 외부 문자열을 Enum으로 변환한다
 */
enum class CareerType {
    NEW,
    EXPERIENCED,
    BOTH,
    UNKNOWN;

    companion object {

        /**
         * LLM 응답 문자열을 CareerType으로 정규화
         *
         * - 매칭 실패 시 UNKNOWN 반환
         * - 예외를 던지지 않는다 (중요)
         */
        fun from(raw: String?): CareerType {
            if (raw.isNullOrBlank()) return UNKNOWN

            return when {
                raw.contains("신입") -> NEW
                raw.contains("경력") -> EXPERIENCED
                raw.contains("무관") -> BOTH
                else -> UNKNOWN
            }
        }
    }
}

