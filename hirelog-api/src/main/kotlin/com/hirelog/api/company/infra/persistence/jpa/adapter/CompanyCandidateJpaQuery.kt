package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyCandidateQuery
import com.hirelog.api.company.application.view.CompanyCandidateView
import com.hirelog.api.company.domain.CompanyCandidateStatus
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyCandidateJpaQueryDslImpl
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyCandidateJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * CompanyCandidateJpaQueryAdapter
 *
 * 책임:
 * - CompanyCandidateQuery 포트 구현
 * - QueryDSL 구현체로 위임
 *
 * 주의:
 * - 정책 / 조건 / 가공 로직 절대 포함 금지
 */
@Component
class CompanyCandidateJpaQuery(
    private val queryDsl: CompanyCandidateJpaQueryDslImpl,
    private val repository: CompanyCandidateJpaRepository
) : CompanyCandidateQuery {

    override fun findViewById(id: Long): CompanyCandidateView? {
        return queryDsl.findViewById(id)
    }

    override fun findAllViewsByBrandId(
        brandId: Long,
        pageable: Pageable
    ): Page<CompanyCandidateView> {
        return queryDsl.findAllViewsByBrandId(
            brandId = brandId,
            pageable = pageable
        )
    }

    override fun findAllViewsByStatus(
        status: CompanyCandidateStatus,
        pageable: Pageable
    ): Page<CompanyCandidateView> {
        return queryDsl.findAllViewsByStatus(
            status = status,
            pageable = pageable
        )
    }

    override fun existsByBrandIdAndNormalizedName(brandId: Long, normalizedName: String): Boolean {
        return repository.existsByBrandIdAndNormalizedName(
            brandId = brandId,
            normalizedName = normalizedName
        )
    }
}
