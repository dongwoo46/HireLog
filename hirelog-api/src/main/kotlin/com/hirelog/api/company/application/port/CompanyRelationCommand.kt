package com.hirelog.api.company.application.port

import com.hirelog.api.company.domain.CompanyRelation

/**
 * CompanyRelation Write Port
 *
 * 책임:
 * - 회사 관계 영속화
 */
interface CompanyRelationCommand {

    fun save(relation: CompanyRelation): CompanyRelation

    fun delete(relation: CompanyRelation)
}
