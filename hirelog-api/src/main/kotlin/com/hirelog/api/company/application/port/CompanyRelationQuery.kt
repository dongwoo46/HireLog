package com.hirelog.api.company.application.port

import com.hirelog.api.company.application.view.CompanyRelationView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * CompanyRelationQuery
 *
 * 책임:
 * - 회사 관계 조회 전용
 */
interface CompanyRelationQuery {

    fun findAllByParentCompanyId(
        parentCompanyId: Long,
        pageable: Pageable
    ): Page<CompanyRelationView>

    fun findAllByChildCompanyId(
        childCompanyId: Long,
        pageable: Pageable
    ): Page<CompanyRelationView>

    fun findView(
        parentCompanyId: Long,
        childCompanyId: Long
    ): CompanyRelationView?
}
