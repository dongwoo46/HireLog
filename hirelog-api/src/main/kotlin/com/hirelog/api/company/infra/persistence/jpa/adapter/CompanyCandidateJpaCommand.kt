package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyCandidateCommand
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.CompanyCandidateStatus
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyCandidateJpaRepository
import org.springframework.stereotype.Component
import org.springframework.data.domain.PageRequest
@Component
class CompanyCandidateJpaCommand(
    private val repository: CompanyCandidateJpaRepository
) : CompanyCandidateCommand {

    /**
     * 신규 생성 / 변경 사항 반영
     *
     * - Dirty Checking 기반
     * - 트랜잭션은 Application Layer 책임
     */
    override fun save(candidate: CompanyCandidate): CompanyCandidate {
        return repository.save(candidate)
    }

    /**
     * 상태 변경 목적 단건 조회
     *
     * - 비관적 락 보장
     */
    override fun findByIdForUpdate(candidateId: Long): CompanyCandidate? {
        return repository.findByIdForUpdate(candidateId)
    }

    override fun findApprovedForUpdate(limit: Int): List<CompanyCandidate> {
        return repository.findApprovedForUpdate(
            status = CompanyCandidateStatus.APPROVED,
            pageable = PageRequest.of(0, limit)
        )
    }
}
