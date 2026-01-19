package com.hirelog.api.relation.infra.persistence.jpa.adapter

import com.hirelog.api.relation.application.jobsummary.query.MemberJobSummaryQuery
import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberJobSummaryJpaRepository
import org.springframework.stereotype.Component

/**
 * MemberJobSummary JPA Query Adapter
 */
@Component
class MemberJobSummaryJpaQuery(
    private val repository: MemberJobSummaryJpaRepository
) : MemberJobSummaryQuery {

    override fun findAllByMemberId(memberId: Long): List<MemberJobSummary> {
        return repository.findAllByMemberId(memberId)
    }

    override fun existsByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean {
        return repository.existsByMemberIdAndJobSummaryId(memberId, jobSummaryId)
    }
}
