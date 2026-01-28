package com.hirelog.api.member.infra.persistence.jpa

import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.Member
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * Member JPA Query Adapter
 *
 * 책임:
 * - MemberQuery Port의 JPA 구현
 * - 조회 전용
 */
@Component
class MemberJpaQuery(
    private val memberRepository: MemberJpaRepository,
) : MemberQuery {

    override fun findById(memberId: Long): Member? =
        memberRepository.findByIdOrNull(memberId)

    override fun findByEmail(email: String): Member? =
        memberRepository.findByEmail(email)

    override fun findByUsername(username: String): Member? =
        memberRepository.findByUsername(username)

    override fun existByUsername(username: String): Boolean =
        memberRepository.existsByUsername(username)
}
