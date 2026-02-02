package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyCandidateCommand
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyCandidateJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyCandidateJpaCommand(
    private val repository: CompanyCandidateJpaRepository
) : CompanyCandidateCommand {

    override fun save(candidate: CompanyCandidate) {
        repository.save(candidate)
    }
}
