package com.hirelog.api.position.application.port

import com.hirelog.api.position.domain.PositionCategory

interface PositionCategoryQuery {

    fun findByNormalizedName(normalizedName: String): PositionCategory?

    fun existsByNormalizedName(normalizedName: String): Boolean

    fun findActive(): List<PositionCategory>
}
