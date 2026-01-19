package com.hirelog.api.position.infra.persistence.jpa

import com.hirelog.api.position.application.command.PositionCommand
import com.hirelog.api.position.domain.Position
import org.springframework.stereotype.Component

/**
 * Position JPA Command Adapter
 *
 * 책임:
 * - PositionCommand Port의 JPA 구현
 */
@Component
class PositionJpaCommand(
    private val repository: PositionJpaRepository
) : PositionCommand {

    override fun create(
        name: String,
        normalizedName: String,
        description: String?
    ): Position {
        require(!repository.existsByNormalizedName(normalizedName)) {
            "Position already exists: $normalizedName"
        }

        val position = Position.create(
            name = name,
            normalizedName = normalizedName,
            description = description
        )

        return repository.save(position)
    }

    override fun activate(positionId: Long) {
        getOrThrow(positionId).activate()
    }

    override fun deprecate(positionId: Long) {
        getOrThrow(positionId).deprecate()
    }

    private fun getOrThrow(positionId: Long): Position =
        repository.findById(positionId)
            .orElseThrow { IllegalArgumentException("Position not found: $positionId") }
}
