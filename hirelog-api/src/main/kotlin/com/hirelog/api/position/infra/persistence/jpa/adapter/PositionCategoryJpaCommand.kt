package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionCategoryCommand
import com.hirelog.api.position.domain.PositionCategory
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionCategoryJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * PositionCategory JPA Command Adapter
 *
 * 책임:
 * - PositionCategoryCommand Port의 JPA 구현
 * - Entity 저장/조회
 */
@Component
class PositionCategoryJpaCommand(
    private val repository: PositionCategoryJpaRepository
) : PositionCategoryCommand {

    override fun save(positionCategory: PositionCategory): PositionCategory =
        repository.save(positionCategory)

    override fun findById(id: Long): PositionCategory? =
        repository.findByIdOrNull(id)

    override fun findByNormalizedName(normalizedName: String): PositionCategory? =
        repository.findByNormalizedName(normalizedName)
}
