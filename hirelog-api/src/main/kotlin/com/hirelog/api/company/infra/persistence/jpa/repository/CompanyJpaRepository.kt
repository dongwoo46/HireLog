package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.domain.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CompanyJpaRepository : JpaRepository<Company, Long> {

    fun findByNormalizedName(normalizedName: String): Company?

    fun existsByNormalizedName(normalizedName: String): Boolean

}
