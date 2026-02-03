package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyRelationQuery
import com.hirelog.api.company.application.view.CompanyRelationView
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyRelationJpaQueryDslImpl
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * CompanyRelationJpaQueryAdapter
 *
 * 책임:
 * - CompanyRelationQuery 포트 구현
 * - QueryDSL 조회 구현체로 위임
 *
 * 주의:
 * - 정책 ❌
 * - 조건 분기 ❌
 * - 가공 로직 ❌
 */
@Component
class CompanyRelationJpaQuery(
    private val queryDsl: CompanyRelationJpaQueryDslImpl
) : CompanyRelationQuery {

    override fun findAllByParentCompanyId(
        parentCompanyId: Long,
        pageable: Pageable
    ): Page<CompanyRelationView> {
        return queryDsl.findAllByParentCompanyId(
            parentCompanyId = parentCompanyId,
            pageable = pageable
        )
    }

    override fun findAllByChildCompanyId(
        childCompanyId: Long,
        pageable: Pageable
    ): Page<CompanyRelationView> {
        return queryDsl.findAllByChildCompanyId(
            childCompanyId = childCompanyId,
            pageable = pageable
        )
    }

    override fun findView(
        parentCompanyId: Long,
        childCompanyId: Long
    ): CompanyRelationView? {
        return queryDsl.findView(
            parentCompanyId = parentCompanyId,
            childCompanyId = childCompanyId
        )
    }
}
