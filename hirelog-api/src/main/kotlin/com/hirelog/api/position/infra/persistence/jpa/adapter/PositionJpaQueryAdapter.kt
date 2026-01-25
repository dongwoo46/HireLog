package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.query.PositionView
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
 * - Entity → View 변환
 */
@Component
class PositionJpaQueryAdapter(
    private val repository: PositionJpaRepository
) : PositionQuery {

    override fun findById(
        id: Long
    ): PositionView? =
        repository.findByIdOrNull(id)
            ?.toView()

    override fun findByNormalizedName(
        normalizedName: String
    ): PositionView? =
        repository.findByNormalizedName(normalizedName)
            ?.toView()

    override fun findActive(): List<PositionView> =
        repository.findAllByStatus(PositionStatus.ACTIVE)
            .map { it.toView() }

    /**
     * Entity → View 변환
     *
     * 주의:
     * - Query 계층 내부 전용
     */
    private fun Position.toView(): PositionView =
        PositionView(
            id = id,
            name = name,
            normalizedName = normalizedName,
            status = status,
            description = description
        )
}
