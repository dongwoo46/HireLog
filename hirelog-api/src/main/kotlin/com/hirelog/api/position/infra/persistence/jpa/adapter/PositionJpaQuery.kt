package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.view.PositionDetailView
import com.hirelog.api.position.application.view.PositionSummaryView
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionJpaQueryDsl
import com.hirelog.api.common.application.port.PagedResult
import org.springframework.stereotype.Component

/**
 * Position JPA Query Adapter
 *
 * 책임:
 * - PositionQuery Port 구현
 * - QueryDSL 구현체로 위임
 */
@Component
class PositionJpaQuery(
    private val queryDsl: PositionJpaQueryDsl
) : PositionQuery {

    override fun findAllPaged(page: Int, size: Int): PagedResult<PositionSummaryView> =
        queryDsl.findAllPaged(page, size)

    override fun findDetailById(id: Long): PositionDetailView? =
        queryDsl.findDetailById(id)

    override fun findByNormalizedName(normalizedName: String): PositionSummaryView? =
        queryDsl.findByNormalizedName(normalizedName)

    override fun findActiveNames(): List<String> =
        queryDsl.findActiveNames()

    override fun existsById(id: Long): Boolean =
        queryDsl.existsById(id)

    override fun existsByNormalizedName(normalizedName: String): Boolean =
        queryDsl.existsByNormalizedName(normalizedName)
}
