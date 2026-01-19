package com.hirelog.api.brand.infra.persistence.jpa

import com.hirelog.api.brand.application.query.BrandQuery
import com.hirelog.api.brand.domain.Brand
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * Brand JPA Query Adapter
 *
 * 책임:
 * - BrandQuery Port의 JPA 구현
 * - 조회 전용
 */
@Component
class BrandJpaQuery(
    private val brandRepository: BrandJpaRepository
) : BrandQuery {

    /**
     * ID 기준 조회
     */
    override fun findById(brandId: Long): Brand? {
        return brandRepository.findByIdOrNull(brandId)
    }

    /**
     * normalizedName 기준 조회
     */
    override fun findByNormalizedName(normalizedName: String): Brand? {
        return brandRepository.findByNormalizedName(normalizedName)
    }
}
