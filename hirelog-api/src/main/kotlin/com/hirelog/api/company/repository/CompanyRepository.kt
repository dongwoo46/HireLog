package com.hirelog.api.company.repository

import com.hirelog.api.company.domain.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {

    fun findByNormalizedName(
        normalizedName: String
    ): Company?

    fun existsByNormalizedName(
        normalizedName: String
    ): Boolean
}
