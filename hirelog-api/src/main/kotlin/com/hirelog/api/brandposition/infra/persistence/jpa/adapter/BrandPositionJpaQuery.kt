package com.hirelog.api.brandposition.infra.persistence.jpa.adapter

import com.hirelog.api.brandposition.application.port.BrandPositionQuery
import com.hirelog.api.brandposition.domain.BrandPosition
import com.hirelog.api.brandposition.infra.persistence.jpa.repository.BrandPositionJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * BrandPosition JPA Query Adapter
 *
 * 역할:
 * - BrandPositionQuery Port의 JPA 구현체
 * - 조회 전용 책임 수행
 */
@Component
class BrandPositionJpaQuery(
    private val repository: BrandPositionJpaRepository
) : BrandPositionQuery {

    override fun findAllByBrandId(brandId: Long): List<BrandPosition> =
        repository.findAllByBrandId(brandId)

    override fun findByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): BrandPosition? =
        repository.findByBrandIdAndPositionId(brandId, positionId)

    override fun findById(id: Long): BrandPosition? =
        repository.findByIdOrNull(id)
}
