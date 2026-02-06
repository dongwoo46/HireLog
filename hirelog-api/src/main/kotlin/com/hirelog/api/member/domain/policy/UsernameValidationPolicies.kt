package com.hirelog.api.member.domain.policy

/**
 * Username 검증 정책 모음 (Domain)
 *
 * 책임:
 * - 검증 정책 정의
 * - 정책 객체 제공
 *
 * 비책임:
 * - 이메일/환경/Admin 판단 ❌
 */
object UsernameValidationPolicies {

    /**
     * 일반 사용자용 정책
     */
    val STRICT: UsernameValidationPolicy = StrictUsernameValidationPolicy

    /**
     * 내부 / 시스템 계정용 우회 정책
     */
    val BYPASS: UsernameValidationPolicy = object : UsernameValidationPolicy {
        override fun validate(username: String) {
            // intentionally empty
        }
    }
}
