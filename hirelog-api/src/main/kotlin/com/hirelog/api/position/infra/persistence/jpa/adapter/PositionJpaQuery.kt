package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.query.PositionQuery
import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.domain.PositionStatus
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * Position JPA Query Adapter
 *
 * 책임:
 * - PositionQuery Port 구현
 * - 조회 전용
 */
@Component
class PositionJpaQuery(
    private val repository: PositionJpaRepository
) : PositionQuery {

    override fun findById(positionId: Long): Position? =
        repository.findByIdOrNull(positionId)

    override fun findByNormalizedName(normalizedName: String): Position? =
        repository.findByNormalizedName(normalizedName)

    override fun findActive(): List<Position> =
        repository.findAllByStatus(PositionStatus.ACTIVE)
}
