package com.hirelog.api.relation.infra.persistence.jpa.adapter

import com.hirelog.api.relation.application.jobsummary.command.MemberJobSummaryCommand
import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberJobSummaryJpaRepository
import org.springframework.stereotype.Component

/**
 * MemberJobSummary JPA Command Adapter
 */
@Component
class MemberJobSummaryJpaCommand(
    private val repository: MemberJobSummaryJpaRepository
) : MemberJobSummaryCommand {

    override fun save(memberJobSummary: MemberJobSummary): MemberJobSummary {
        return repository.save(memberJobSummary)
    }

    override fun delete(memberJobSummary: MemberJobSummary) {
        repository.delete(memberJobSummary)
    }
}
