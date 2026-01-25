package com.hirelog.api.position.application.port

import com.hirelog.api.position.domain.Position

/**
 * LoadPositionPort
 *
 * 책임:
 * - Write 유스케이스를 위한 Position 조회
 *
 * 특징:
 * - Entity 반환
 * - 조회 목적은 오직 쓰기 판단 보조
 *
 * 금지:
 * - View 반환 ❌
 * - 외부 API 노출 ❌
 */
interface PositionLoad {

    /**
     * ID 기준 Position 조회
     */
    fun loadById(positionId: Long): Position?

    /**
     * 정규화된 포지션명 기준 조회
     *
     * 용도:
     * - getOrCreate
     * - 중복 검증
     */
    fun loadByNormalizedName(normalizedName: String): Position?
}
