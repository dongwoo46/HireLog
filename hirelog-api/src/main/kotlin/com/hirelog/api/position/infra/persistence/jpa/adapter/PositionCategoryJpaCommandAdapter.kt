package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionCategoryCommand
import com.hirelog.api.position.domain.PositionCategory
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionCategoryJpaRepository
import org.springframework.stereotype.Component

@Component
class PositionCategoryJpaCommandAdapter(
    private val repository: PositionCategoryJpaRepository
) : PositionCategoryCommand {

    override fun save(positionCategory: PositionCategory): PositionCategory =
        repository.save(positionCategory)
}
