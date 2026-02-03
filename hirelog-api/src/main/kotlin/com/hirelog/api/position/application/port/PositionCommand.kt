package com.hirelog.api.position.application.port

import com.hirelog.api.position.domain.Position

/**
 * Position Command Port
 *
 * 책임:
 * - Position 영속화 추상화
 * - Write 유스케이스를 위한 Entity 조회
 */
interface PositionCommand {

    fun save(position: Position): Position

    fun delete(position: Position)

    fun findById(id: Long): Position?

    fun findByNormalizedName(normalizedName: String): Position?
}
