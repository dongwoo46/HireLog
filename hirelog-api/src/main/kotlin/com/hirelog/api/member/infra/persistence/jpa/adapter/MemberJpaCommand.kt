package com.hirelog.api.member.infra.persistence.jpa.adapter

import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.infra.persistence.jpa.repository.MemberJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * Member JPA Command Adapter
 *
 * 책임:
 * - MemberCommand Port의 JPA 구현
 * - Entity 저장/조회
 */
@Component
class MemberJpaCommand(
    private val memberRepository: MemberJpaRepository,
) : MemberCommand {

    override fun save(member: Member): Member =
        memberRepository.save(member)

    override fun findById(id: Long): Member? =
        memberRepository.findByIdOrNull(id)

    override fun findByEmail(email: String): Member? =
        memberRepository.findByEmail(email)

    override fun findByUsername(username: String): Member? =
        memberRepository.findByUsername(username)
}
