package com.hirelog.api.member.domain.policy

/**
 * Username 검증 정책
 *
 * 도메인 규칙:
 * - username 검증 방식의 변경 가능성을 흡수
 */
interface UsernameValidationPolicy {

    fun validate(username: String)
}
