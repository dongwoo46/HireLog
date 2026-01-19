package com.hirelog.api.company.application.query

import com.hirelog.api.company.domain.CompanyRelation

/**
 * CompanyRelation Query Port
 *
 * 책임:
 * - 회사 관계 조회 전용
 */
interface CompanyRelationQuery {

    fun findByParentCompanyId(parentCompanyId: Long): List<CompanyRelation>

    fun findByChildCompanyId(childCompanyId: Long): List<CompanyRelation>

    fun findRelation(
        parentCompanyId: Long,
        childCompanyId: Long
    ): CompanyRelation?
}
