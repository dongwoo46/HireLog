package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyCandidateQuery
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.CompanyCandidateStatus
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyCandidateJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyCandidateJpaQuery(
    private val repository: CompanyCandidateJpaRepository
) : CompanyCandidateQuery {

    override fun findById(id: Long): CompanyCandidate? {
        return repository.findById(id).orElse(null)
    }

    override fun findAllByBrandId(brandId: Long): List<CompanyCandidate> {
        return repository.findAllByBrandId(brandId)
    }

    override fun findAllByStatus(status: CompanyCandidateStatus): List<CompanyCandidate> {
        return repository.findAllByStatus(status)
    }

    override fun findByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): CompanyCandidate? {
        return repository.findByBrandIdAndNormalizedName(
            brandId = brandId,
            normalizedName = normalizedName
        )
    }
}
