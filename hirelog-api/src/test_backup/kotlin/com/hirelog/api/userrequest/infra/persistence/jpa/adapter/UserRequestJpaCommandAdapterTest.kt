package com.hirelog.api.userrequest.infra.persistence.jpa.adapter

import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.infra.persistence.jpa.repository.UserRequestJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UserRequestJpaCommandAdapter 테스트")
class UserRequestJpaCommandAdapterTest {

    private val repository: UserRequestJpaRepository = mockk()
    private val adapter = UserRequestJpaCommandAdapter(repository)

    @Test
    @DisplayName("save: Repository에 위임해야 한다")
    fun save() {
        // given
        val request = mockk<UserRequest>()
        every { repository.save(request) } returns request

        // when
        val result = adapter.save(request)

        // then
        assertEquals(request, result)
        verify(exactly = 1) { repository.save(request) }
    }
}
