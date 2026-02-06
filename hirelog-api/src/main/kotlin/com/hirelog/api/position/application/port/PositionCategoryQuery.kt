package com.hirelog.api.position.application.port

import com.hirelog.api.position.application.view.PositionCategoryView
import com.hirelog.api.common.application.port.PagedResult

/**
 * PositionCategory Query Port
 *
 * 책임:
 * - PositionCategory 조회 계약 정의 (View 반환)
 * - QueryDSL 기반 구현
 */
interface PositionCategoryQuery {

    fun findAllPaged(page: Int, size: Int): PagedResult<PositionCategoryView>

    fun findDetailById(id: Long): PositionCategoryView?

    fun existsById(id: Long): Boolean

    fun existsByNormalizedName(normalizedName: String): Boolean
}
