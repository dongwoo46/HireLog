package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.domain.CompanyRelation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CompanyRelationJpaRepository : JpaRepository<CompanyRelation, Long> {

    fun existsByParentCompanyIdAndChildCompanyId(
        parentCompanyId: Long,
        childCompanyId: Long
    ): Boolean
}
