package com.hirelog.api.company.repository

import com.hirelog.api.company.domain.Position
import org.springframework.data.jpa.repository.JpaRepository

interface PositionRepository : JpaRepository<Position, Long> {

    fun findByCompanyIdAndNormalizedName(
        companyId: Long,
        normalizedName: String
    ): Position?

    fun existsByCompanyIdAndNormalizedName(
        companyId: Long,
        normalizedName: String
    ): Boolean
}
