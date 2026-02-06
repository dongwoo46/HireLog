package com.hirelog.api.position.application.port

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.position.application.view.PositionCategoryDetailView
import com.hirelog.api.position.application.view.PositionCategoryListView
import com.hirelog.api.position.domain.PositionStatus
import org.springframework.data.domain.Pageable

interface PositionCategoryQuery {

    /**
     * PositionCategory 목록 조회 (검색/필터링)
     *
     * @param status 상태 필터 (ACTIVE / INACTIVE)
     * @param name   이름 검색 (부분 일치)
     */
    fun findAll(
        status: PositionStatus?,
        name: String?,
        pageable: Pageable
    ): PagedResult<PositionCategoryListView>

    fun findDetailById(id: Long): PositionCategoryDetailView?

    fun existsById(id: Long): Boolean

    fun existsByNormalizedName(normalizedName: String): Boolean
}

