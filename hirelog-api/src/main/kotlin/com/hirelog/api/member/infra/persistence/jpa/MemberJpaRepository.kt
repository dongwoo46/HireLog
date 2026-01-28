package com.hirelog.api.member.infra.persistence.jpa

import com.hirelog.api.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<Member, Long> {

    fun findByEmail(email: String): Member?

    fun findByUsername(username:String): Member?

    fun existsByUsername(username: String): Boolean
}
