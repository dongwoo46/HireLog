package com.hirelog.api.company.repository

import com.hirelog.api.company.domain.Brand
import org.springframework.data.jpa.repository.JpaRepository

interface BrandRepository : JpaRepository<Brand, Long> {

    fun findByNormalizedName(normalizedName: String): Brand?
}
