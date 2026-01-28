package com.hirelog.api.member.infra.persistence.jpa

import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.domain.Member
import org.springframework.stereotype.Component

/**
 * Member JPA Command Adapter
 *
 * 책임:
 * - MemberCommand Port의 JPA 구현
 */
@Component
class MemberJpaCommand(
    private val memberRepository: MemberJpaRepository,
) : MemberCommand {

    override fun save(member: Member): Member =
        memberRepository.save(member)
}
