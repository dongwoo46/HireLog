package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.position.application.port.PositionCategoryQuery
import com.hirelog.api.position.application.view.PositionCategoryDetailView
import com.hirelog.api.position.application.view.PositionCategoryListView
import com.hirelog.api.position.domain.PositionStatus
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionCategoryJpaQueryDsl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class PositionCategoryJpaQuery(
    private val queryDsl: PositionCategoryJpaQueryDsl
) : PositionCategoryQuery {

    override fun findAll(
        status: PositionStatus?,
        name: String?,
        pageable: Pageable
    ): PagedResult<PositionCategoryListView> =
        queryDsl.findAll(status, name, pageable)

    override fun findDetailById(id: Long): PositionCategoryDetailView? =
        queryDsl.findDetailById(id)

    override fun existsById(id: Long): Boolean =
        queryDsl.existsById(id)

    override fun existsByNormalizedName(normalizedName: String): Boolean =
        queryDsl.existsByNormalizedName(normalizedName)
}
