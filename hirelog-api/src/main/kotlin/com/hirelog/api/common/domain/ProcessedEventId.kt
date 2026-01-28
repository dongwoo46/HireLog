package com.hirelog.api.common.domain

/**
 * ProcessedEventId
 *
 * 의미:
 * - 비즈니스 이벤트를 식별하는 값 객체
 * - Kafka 멱등 처리의 기준 키
 *
 * 역할:
 * - 단순 String 식별자와 의미적으로 구분
 * - 잘못된 식별자 전달을 방지
 *
 * 제약:
 * - 불변 객체
 * - 저장 기술(JPA 등)과 무관
 */
class ProcessedEventId private constructor(
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "ProcessedEventId must not be blank"
        }
    }

    companion object {

        /**
         * 문자열 기반 식별자 생성
         */
        fun from(value: String): ProcessedEventId {
            return ProcessedEventId(value)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedEventId) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value
    }
}
