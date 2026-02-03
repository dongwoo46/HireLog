package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyCommand
import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyJpaCommand(
    private val repository: CompanyJpaRepository
) : CompanyCommand {

    /**
     * 신규 / 변경 저장
     *
     * - Dirty Checking 전제
     * - 트랜잭션은 Application Service 책임
     */
    override fun save(company: Company):Company {
        return repository.save(company)
    }

    /**
     * 수정 목적 단건 조회
     *
     * - 락 없음
     * - verify / deactivate 등 idempotent 상태 변경 전제
     */
    override fun findById(id: Long): Company? {
        return repository.findById(id).orElse(null)
    }

    override fun findByNormalizedName(normalizedName: String): Company? {
        return repository.findByNormalizedName(normalizedName)
    }
}
