package com.hirelog.api.member.infra.persistence.jpa.adapter

import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.infra.persistence.jpa.repository.MemberJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("MemberJpaCommand Adapter 테스트")
class MemberJpaCommandTest {

    private val repository: MemberJpaRepository = mockk()
    private val adapter = MemberJpaCommand(repository)

    @Test
    @DisplayName("모든 메서드가 Repository에 올바르게 위임되어야 한다")
    fun delegation_test() {
        // given
        val member = mockk<Member>()
        every { repository.save(any()) } returns member
        every { repository.findById(1L) } returns Optional.of(member)
        every { repository.findByEmail("email") } returns member
        every { repository.findByUsername("user") } returns member

        // when & then
        adapter.save(member)
        verify(exactly = 1) { repository.save(member) }

        adapter.findById(1L)
        verify(exactly = 1) { repository.findById(1L) }

        adapter.findByEmail("email")
        verify(exactly = 1) { repository.findByEmail("email") }

        adapter.findByUsername("user")
        verify(exactly = 1) { repository.findByUsername("user") }
    }
}
