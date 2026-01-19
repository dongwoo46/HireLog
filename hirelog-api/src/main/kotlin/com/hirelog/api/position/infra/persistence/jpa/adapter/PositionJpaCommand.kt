package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.command.PositionCommand
import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionJpaRepository
import org.springframework.stereotype.Component

/**
 * Position JPA Command Adapter
 *
 * 책임:
 * - PositionCommand Port의 JPA 구현
 * - 영속화만 수행
 */
@Component
class PositionJpaCommand(
    private val repository: PositionJpaRepository
) : PositionCommand {

    override fun save(position: Position): Position {
        return repository.save(position)
    }

    override fun delete(position: Position) {
        repository.delete(position)
    }
}
