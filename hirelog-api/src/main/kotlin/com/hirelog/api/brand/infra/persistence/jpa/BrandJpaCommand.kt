package com.hirelog.api.brand.infra.persistence.jpa

import com.hirelog.api.brand.application.command.BrandCommand
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import org.springframework.stereotype.Component

/**
 * Brand JPA Command Adapter
 *
 * 책임:
 * - BrandCommand Port의 JPA 구현
 * - Entity 생성/상태 변경을 영속화
 */
@Component
class BrandJpaCommand(
    private val brandRepository: BrandJpaRepository
) : BrandCommand {

    /**
     * Brand 생성
     */
    override fun create(
        name: String,
        normalizedName: String,
        companyId: Long?,
        source: BrandSource
    ): Brand {
        require(!brandRepository.existsByNormalizedName(normalizedName)) {
            "Brand already exists: $normalizedName"
        }

        val brand = Brand.create(
            name = name,
            normalizedName = normalizedName,
            companyId = companyId,
            source = source
        )

        return brandRepository.save(brand)
    }

    /**
     * 브랜드 검증 승인
     */
    override fun verify(brandId: Long) {
        val brand = getOrThrow(brandId)
        brand.verify()
    }

    /**
     * 브랜드 검증 거절
     */
    override fun reject(brandId: Long) {
        val brand = getOrThrow(brandId)
        brand.reject()
    }

    /**
     * 브랜드 비활성화
     */
    override fun deactivate(brandId: Long) {
        val brand = getOrThrow(brandId)
        brand.deactivate()
    }

    /**
     * 내부 공통 조회
     *
     * 주의:
     * - infra 내부에서만 사용
     */
    private fun getOrThrow(brandId: Long): Brand =
        brandRepository.findById(brandId)
            .orElseThrow { IllegalArgumentException("Brand not found: $brandId") }
}
