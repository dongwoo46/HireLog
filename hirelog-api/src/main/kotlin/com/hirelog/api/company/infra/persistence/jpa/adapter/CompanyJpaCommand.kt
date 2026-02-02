package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyCommand
import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyJpaRepository
import org.springframework.stereotype.Component

/**
 * Company JPA Command Adapter
 *
 * 역할:
 * - CompanyCommand 포트를 JPA로 구현
 * - 영속성 세부 사항을 infra 계층에 격리
 */
@Component
class CompanyJpaCommand(
    private val companyRepository: CompanyJpaRepository
) : CompanyCommand {

    /**
     * Company 저장
     *
     * - 신규 생성
     * - 상태 변경 반영
     */
    override fun save(company: Company): Company {
        return companyRepository.save(company)
    }
}
