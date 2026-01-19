package com.hirelog.api.relation.infra.persistence.jpa.adapter


import com.hirelog.api.relation.application.company.query.MemberCompanyQuery
import com.hirelog.api.relation.domain.model.MemberCompany
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberCompanyJpaRepository
import org.springframework.stereotype.Component

/**
 * MemberCompany JPA Query Adapter
 */
@Component
class MemberCompanyJpaQuery(
    private val repository: MemberCompanyJpaRepository
) : MemberCompanyQuery {

    override fun findAllByMemberId(memberId: Long): List<MemberCompany> {
        return repository.findAllByMemberId(memberId)
    }

    override fun existsByMemberIdAndCompanyId(
        memberId: Long,
        companyId: Long
    ): Boolean {
        return repository.existsByMemberIdAndCompanyId(memberId, companyId)
    }
}
