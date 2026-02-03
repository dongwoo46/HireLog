package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyCandidateCommand
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyCandidateJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyCandidateJpaCommand(
    private val repository: CompanyCandidateJpaRepository
) : CompanyCandidateCommand {

    /**
     * CompanyCandidate 저장
     *
     * 책임:
     * - 신규 엔티티 저장
     * - 변경된 엔티티 반영 (Dirty Checking)
     *
     * 주의:
     * - 호출자는 트랜잭션 컨텍스트를 보장해야 함
     */
    override fun save(candidate: CompanyCandidate): CompanyCandidate {
        return repository.save(candidate)
    }

    /**
     * 수정 목적 단건 조회
     *
     * 책임:
     * - 상태 변경(approve / reject) 전용 조회
     *
     * 특징:
     * - 비관적 락(PESSIMISTIC_WRITE) 적용
     * - 동시 수정 방지
     *
     * 주의:
     * - 반드시 트랜잭션 내부에서 호출되어야 함
     * - 존재 보장/예외 처리는 Application Service 책임
     */
    override fun findByIdForUpdate(id: Long): CompanyCandidate? {
        return repository.findByIdForUpdate(id)
    }

    override fun findByNormalizedName(normalizedName: String): CompanyCandidate? {
        return repository.findByNormalizedName(normalizedName)
    }
}
