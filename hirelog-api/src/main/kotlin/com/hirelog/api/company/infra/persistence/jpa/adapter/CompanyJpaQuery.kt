package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyJpaRepository
import org.springframework.stereotype.Component

/**
 * Company JPA Query Adapter
 *
 * 역할:
 * - CompanyQuery 포트를 JPA로 구현
 * - 조회 로직만 담당 (절대 예외 던지지 않음)
 */
@Component
class CompanyJpaQuery(
    private val companyRepository: CompanyJpaRepository
) : CompanyQuery {

    override fun findById(companyId: Long): Company? =
        companyRepository.findById(companyId).orElse(null)

    override fun findByNormalizedName(normalizedName: String): Company? =
        companyRepository.findByNormalizedName(normalizedName)

    override fun findAllNames(): List<String> =
        companyRepository.findAll().map { it.name }
}
