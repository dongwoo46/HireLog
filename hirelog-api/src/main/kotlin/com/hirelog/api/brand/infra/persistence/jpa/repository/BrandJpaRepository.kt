package com.hirelog.api.brand.infra.persistence.jpa.repository

import com.hirelog.api.brand.domain.Brand
import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<Brand, Long> {

    fun existsByNormalizedName(normalizedName: String): Boolean

    fun findByNormalizedName(normalizedName: String): Brand?
}
