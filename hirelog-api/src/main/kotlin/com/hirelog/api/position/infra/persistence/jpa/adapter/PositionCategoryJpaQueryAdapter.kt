package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionCategoryQuery
import com.hirelog.api.position.domain.PositionCategory
import com.hirelog.api.position.domain.PositionCategoryStatus
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionCategoryJpaRepository
import org.springframework.stereotype.Component

@Component
class PositionCategoryJpaQueryAdapter(
    private val repository: PositionCategoryJpaRepository
) : PositionCategoryQuery {

    override fun findByNormalizedName(normalizedName: String): PositionCategory? =
        repository.findByNormalizedName(normalizedName)

    override fun existsByNormalizedName(normalizedName: String): Boolean =
        repository.existsByNormalizedName(normalizedName)

    override fun findActive(): List<PositionCategory> =
        repository.findByStatus(PositionCategoryStatus.ACTIVE)
}
