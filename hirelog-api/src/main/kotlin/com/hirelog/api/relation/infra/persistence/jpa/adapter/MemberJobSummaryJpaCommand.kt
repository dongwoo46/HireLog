package com.hirelog.api.relation.infra.persistence.adapter

import com.hirelog.api.relation.application.memberjobsummary.MemberJobSummaryCommand
import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberJobSummaryJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * MemberJobSummary Command Adapter
 *
 * 책임:
 * - Write Port 구현
 * - JPA Repository 위임
 */
@Component
class MemberJobSummaryJpaCommand(
    private val jpaRepository: MemberJobSummaryJpaRepository
) : MemberJobSummaryCommand {

    override fun findEntityByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummary? {
        return jpaRepository.findByMemberIdAndJobSummaryId(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        )
    }

    override fun save(memberJobSummary: MemberJobSummary) {
        jpaRepository.save(memberJobSummary)
    }
}
