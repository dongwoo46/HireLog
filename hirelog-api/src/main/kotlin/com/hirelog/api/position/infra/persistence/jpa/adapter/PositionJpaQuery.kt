package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.view.PositionDetailView
import com.hirelog.api.position.application.view.PositionListView
import com.hirelog.api.position.application.view.PositionView
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionJpaQueryDsl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * PositionJpaQuery
 *
 * 책임:
 * - PositionQuery Port 구현
 * - QueryDSL 구현체로 위임
 */
@Component
class PositionJpaQuery(
    private val queryDsl: PositionJpaQueryDsl
) : PositionQuery {

    override fun findAll(
        status: String?,
        categoryId: Long?,
        name: String?,
        pageable: Pageable
    ): PagedResult<PositionListView> =
        queryDsl.findAll(status, categoryId, name, pageable)

    override fun findDetailById(id: Long): PositionDetailView? =
        queryDsl.findDetailById(id)

    override fun findByNormalizedName(normalizedName: String): PositionView? =
        queryDsl.findByNormalizedName(normalizedName)

    override fun findActiveNames(): List<String> =
        queryDsl.findActiveNames()

    override fun existsById(id: Long): Boolean =
        queryDsl.existsById(id)

    override fun existsByNormalizedName(normalizedName: String): Boolean =
        queryDsl.existsByNormalizedName(normalizedName)
}

