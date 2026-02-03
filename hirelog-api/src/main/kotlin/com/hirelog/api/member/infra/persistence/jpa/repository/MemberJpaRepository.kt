package com.hirelog.api.member.infra.persistence.jpa.repository

import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.domain.MemberStatus
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<Member, Long> {

    fun findByEmail(email: String): Member?

    fun findByUsername(username:String): Member?

    fun existsByUsername(username: String): Boolean

    fun existsByIdAndStatus(
        id: Long,
        status: MemberStatus
    ): Boolean

    fun existsByEmail(email: String): Boolean
}
