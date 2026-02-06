package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * Position JPA Command Adapter
 *
 * 책임:
 * - PositionCommand Port의 JPA 구현
 * - Entity 저장/조회
 */
@Component
class PositionJpaCommand(
    private val repository: PositionJpaRepository
) : PositionCommand {

    override fun save(position: Position): Position =
        repository.save(position)

    override fun findById(id: Long): Position? =
        repository.findByIdOrNull(id)

    override fun findByNormalizedName(normalizedName: String): Position? =
        repository.findByNormalizedName(normalizedName)
}
