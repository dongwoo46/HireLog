package com.hirelog.api.brand.infra.persistence.jpa

import com.hirelog.api.brand.domain.Brand
import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<Brand, Long> {

    fun findByNormalizedName(normalizedName: String): Brand?

    fun existsByNormalizedName(normalizedName: String): Boolean
}
