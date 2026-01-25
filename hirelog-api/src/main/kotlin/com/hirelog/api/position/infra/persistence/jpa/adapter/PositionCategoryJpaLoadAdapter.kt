package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionCategoryLoad
import com.hirelog.api.position.domain.PositionCategory
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionCategoryJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PositionCategoryJpaLoadAdapter(
    private val repository: PositionCategoryJpaRepository
) : PositionCategoryLoad {

    override fun getById(id: Long): PositionCategory =
        repository.findByIdOrNull(id)
            ?: throw NoSuchElementException("PositionCategory not found: id=$id")
}
