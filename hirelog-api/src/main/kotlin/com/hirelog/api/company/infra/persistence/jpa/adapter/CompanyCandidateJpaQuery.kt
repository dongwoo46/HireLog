package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyCandidateQuery
import com.hirelog.api.company.application.view.CompanyCandidateDetailView
import com.hirelog.api.company.application.view.CompanyCandidateListView
import com.hirelog.api.company.presentation.controller.dto.CompanyCandidateSearchReq
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyCandidateJpaQueryDslImpl
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyCandidateJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CompanyCandidateJpaQuery(
    private val queryDsl: CompanyCandidateJpaQueryDslImpl,
    private val repository: CompanyCandidateJpaRepository,
) : CompanyCandidateQuery {

    override fun existsByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): Boolean {
        return repository.existsByBrandIdAndNormalizedName(brandId, normalizedName)
    }

    override fun search(
        condition: CompanyCandidateSearchReq,
        pageable: Pageable
    ): Page<CompanyCandidateListView> {
        return queryDsl.search(condition, pageable)
    }

    override fun findDetailById(candidateId: Long): CompanyCandidateDetailView? {
        return queryDsl.findDetailById(candidateId)
    }
}
