package com.hirelog.api.brand.infra.persistence.jpa

import com.hirelog.api.brand.domain.Brand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BrandJpaRepository : JpaRepository<Brand, Long> {

    fun findByNormalizedName(normalizedName: String): Brand?

    fun existsByNormalizedName(normalizedName: String): Boolean

}
