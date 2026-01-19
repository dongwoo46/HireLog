package com.hirelog.api.relation.infra.persistence.jpa.adapter


import com.hirelog.api.relation.application.company.command.MemberCompanyCommand
import com.hirelog.api.relation.domain.model.MemberCompany
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberCompanyJpaRepository
import org.springframework.stereotype.Component

/**
 * MemberCompany JPA Command Adapter
 */
@Component
class MemberCompanyJpaCommand(
    private val repository: MemberCompanyJpaRepository
) : MemberCompanyCommand {

    override fun save(memberCompany: MemberCompany): MemberCompany {
        return repository.save(memberCompany)
    }
}
