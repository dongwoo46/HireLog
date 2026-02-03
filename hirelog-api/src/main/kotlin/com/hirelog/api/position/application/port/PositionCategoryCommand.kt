package com.hirelog.api.position.application.port

import com.hirelog.api.position.domain.PositionCategory

/**
 * PositionCategory Command Port
 *
 * 책임:
 * - PositionCategory 영속화 추상화
 * - Write 유스케이스를 위한 Entity 조회
 */
interface PositionCategoryCommand {

    fun save(positionCategory: PositionCategory): PositionCategory

    fun findById(id: Long): PositionCategory?

    fun findByNormalizedName(normalizedName: String): PositionCategory?
}
