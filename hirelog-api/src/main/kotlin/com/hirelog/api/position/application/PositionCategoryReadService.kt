package com.hirelog.api.position.application

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.position.application.port.PositionCategoryQuery
import com.hirelog.api.position.application.view.PositionCategoryDetailView
import com.hirelog.api.position.application.view.PositionCategoryListView
import com.hirelog.api.position.domain.PositionStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PositionCategoryReadService(
    private val positionCategoryQuery: PositionCategoryQuery
) {

    fun findDetail(categoryId: Long): PositionCategoryDetailView? =
        positionCategoryQuery.findDetailById(categoryId)

    fun search(
        status: PositionStatus?,
        name: String?,
        page: Int,
        size: Int
    ): PagedResult<PositionCategoryListView> =
        positionCategoryQuery.findAll(
            status = status,
            name = name,
            pageable = PageRequest.of(page, size)
        )
}
