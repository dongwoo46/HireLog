package com.hirelog.api.member.application.port

import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.member.domain.Member

/**
 * Member Read Port
 *
 * 책임:
 * - Member 조회 계약 정의
 * - 조회 기술(JPA 등)과 분리
 */
interface MemberQuery {

    fun findById(memberId: Long): Member?

    fun findByEmail(email: String): Member?

    fun findByUsername(username: String): Member?

    fun existByUsername(username: String): Boolean
}
