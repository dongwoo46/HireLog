package com.hirelog.api.company.repository

import com.hirelog.api.company.domain.Position
import org.springframework.data.jpa.repository.JpaRepository

interface PositionRepository : JpaRepository<Position, Long> {

    fun findByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): Position?

    fun existsByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): Boolean
}
