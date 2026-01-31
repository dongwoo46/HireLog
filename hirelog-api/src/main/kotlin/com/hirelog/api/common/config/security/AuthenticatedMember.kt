package com.hirelog.api.common.config.security

import com.hirelog.api.member.domain.MemberRole

/**
 * 인증된 사용자 정보
 *
 * 책임:
 * - JWT 기반 인증 주체 표현
 * - Security Context → Controller 전달 객체
 */
data class AuthenticatedMember(
    val memberId: Long,
    val role: MemberRole
)
