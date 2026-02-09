package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.Optional

@DisplayName("PositionJpaCommand Adapter 테스트")
class PositionJpaCommandTest {

    private val repository: PositionJpaRepository = mockk()
    private val adapter = PositionJpaCommand(repository)

    @Test
    @DisplayName("모든 메서드가 Repository에 올바르게 위임되어야 한다")
    fun delegation_test() {
        // given
        val position = mockk<Position>()
        every { repository.save(position) } returns position
        every { repository.delete(position) } returns Unit
        // MockK findByIdOrNull extension might need care, usually mocking findAll/FindById works
        // Spring's findByIdOrNull is an extension on CrudRepository. 
        // We mock the underlying repository.findById
        every { repository.findById(1L) } returns Optional.of(position)
        every { repository.findByNormalizedName("name") } returns position

        // when & then
        adapter.save(position)
        verify(exactly = 1) { repository.save(position) }

        adapter.delete(position)
        verify(exactly = 1) { repository.delete(position) }

        adapter.findById(1L)
        verify(exactly = 1) { repository.findById(1L) }

        adapter.findByNormalizedName("name")
        verify(exactly = 1) { repository.findByNormalizedName("name") }
    }
}
