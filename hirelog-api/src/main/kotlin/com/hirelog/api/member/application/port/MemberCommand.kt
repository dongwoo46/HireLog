package com.hirelog.api.member.application.port

import com.hirelog.api.member.domain.Member

/**
 * Member Command Port
 *
 * 책임:
 * - Member 영속화 추상화
 * - Write 유스케이스를 위한 Entity 조회
 */
interface MemberCommand {

    fun save(member: Member): Member

    fun findById(id: Long): Member?

    fun findByEmail(email: String): Member?

    fun findByUsername(username: String): Member?
}
