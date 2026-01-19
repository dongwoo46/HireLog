package com.hirelog.api.position.application.query

import com.hirelog.api.position.domain.Position

/**
 * Position Read Port
 *
 * 책임:
 * - Position 조회 계약 정의
 * - 조회 기술과 분리
 */
interface PositionQuery {

    /**
     * ID 기준 조회
     */
    fun findById(positionId: Long): Position?

    /**
     * normalizedName 기준 조회
     */
    fun findByNormalizedName(normalizedName: String): Position?

    /**
     * 활성 Position 목록 조회
     */
    fun findActive(): List<Position>
}
