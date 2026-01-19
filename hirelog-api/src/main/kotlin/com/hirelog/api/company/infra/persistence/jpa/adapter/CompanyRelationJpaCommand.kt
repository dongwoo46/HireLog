package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.command.CompanyRelationCommand
import com.hirelog.api.company.domain.CompanyRelation
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyRelationJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyRelationJpaCommand(
    private val relationRepository: CompanyRelationJpaRepository
) : CompanyRelationCommand {

    override fun save(relation: CompanyRelation): CompanyRelation =
        relationRepository.save(relation)

    override fun delete(relation: CompanyRelation) {
        relationRepository.delete(relation)
    }
}
