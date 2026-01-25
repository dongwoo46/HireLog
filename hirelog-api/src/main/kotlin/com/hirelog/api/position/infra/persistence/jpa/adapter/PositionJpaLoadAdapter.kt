package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionLoad
import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * Position JPA Load Adapter
 *
 * 책임:
 * - PositionLoad Port 구현
 * - Write 유스케이스를 위한 Entity 조회
 *
 * 주의:
 * - 상태 변경 ❌
 * - 목록 조회 ❌
 * - 비즈니스 판단 ❌
 */
@Component
class PositionJpaLoadAdapter(
    private val repository: PositionJpaRepository
) : PositionLoad {

    override fun loadById(
        positionId: Long
    ): Position? =
        repository.findByIdOrNull(positionId)

    override fun loadByNormalizedName(
        normalizedName: String
    ): Position? =
        repository.findByNormalizedName(normalizedName)
}
