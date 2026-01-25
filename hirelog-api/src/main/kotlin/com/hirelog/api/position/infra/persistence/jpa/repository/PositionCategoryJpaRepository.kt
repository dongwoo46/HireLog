package com.hirelog.api.position.infra.persistence.jpa.repository

import com.hirelog.api.position.domain.PositionCategory
import com.hirelog.api.position.domain.PositionCategoryStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PositionCategoryJpaRepository : JpaRepository<PositionCategory, Long> {

    fun findByNormalizedName(normalizedName: String): PositionCategory?

    fun existsByNormalizedName(normalizedName: String): Boolean

    fun findByStatus(status: PositionCategoryStatus): List<PositionCategory>
}