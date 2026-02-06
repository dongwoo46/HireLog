package com.hirelog.api.common.config.security

import com.hirelog.api.member.domain.MemberRole

/**
 * 인증된 사용자 정보
 *
 * 책임:
 * - JWT 기반 인증 주체 표현
 * - Security Context → Controller / Application 전달 객체
 * - 권한 스냅샷 제공
 */
data class AuthenticatedMember(
    val memberId: Long,
    val role: MemberRole
) {

    /**
     * 관리자 여부
     *
     * - 요청 시점 기준 권한 스냅샷
     * - DB 재조회 없음
     */
    fun isAdmin(): Boolean {
        return role == MemberRole.ADMIN
    }
}
