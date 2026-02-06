package com.hirelog.api.position.application.port

import com.hirelog.api.position.application.view.PositionDetailView
import com.hirelog.api.position.application.view.PositionSummaryView
import com.hirelog.api.common.application.port.PagedResult

/**
 * Position Query Port
 *
 * 책임:
 * - Position 조회 계약 정의 (View 반환)
 * - QueryDSL 기반 구현
 */
interface PositionQuery {

    fun findAllPaged(page: Int, size: Int): PagedResult<PositionSummaryView>

    fun findDetailById(id: Long): PositionDetailView?

    fun findByNormalizedName(normalizedName: String): PositionSummaryView?

    fun findActiveNames(): List<String>

    fun existsById(id: Long): Boolean

    fun existsByNormalizedName(normalizedName: String): Boolean
}
