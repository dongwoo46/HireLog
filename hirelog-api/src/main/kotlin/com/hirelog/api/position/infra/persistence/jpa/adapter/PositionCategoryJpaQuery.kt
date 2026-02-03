package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionCategoryQuery
import com.hirelog.api.position.application.view.PositionCategoryView
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionCategoryJpaQueryDsl
import com.hirelog.api.userrequest.application.port.PagedResult
import org.springframework.stereotype.Component

/**
 * PositionCategory JPA Query Adapter
 *
 * 책임:
 * - PositionCategoryQuery Port 구현
 * - QueryDSL 구현체로 위임
 */
@Component
class PositionCategoryJpaQuery(
    private val queryDsl: PositionCategoryJpaQueryDsl
) : PositionCategoryQuery {

    override fun findAllPaged(page: Int, size: Int): PagedResult<PositionCategoryView> =
        queryDsl.findAllPaged(page, size)

    override fun findDetailById(id: Long): PositionCategoryView? =
        queryDsl.findDetailById(id)

    override fun existsById(id: Long): Boolean =
        queryDsl.existsById(id)

    override fun existsByNormalizedName(normalizedName: String): Boolean =
        queryDsl.existsByNormalizedName(normalizedName)
}
