package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.application.view.CompanyNameView
import com.hirelog.api.company.application.view.CompanyView
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyJpaQueryDslImpl
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * CompanyJpaQueryAdapter
 *
 * 책임:
 * - CompanyQuery 포트 구현
 * - QueryDSL 기반 조회 로직 위임
 *
 * 주의:
 * - 정책 ❌
 * - 필터링 조건 추가 ❌
 * - 가공 로직 ❌
 */
@Component
class CompanyJpaQuery(
    private val queryDsl: CompanyJpaQueryDslImpl,
    private val repository: CompanyJpaRepository
) : CompanyQuery {

    override fun findViewById(id: Long): CompanyView? {
        return queryDsl.findViewById(id)
    }

    override fun findAllViews(pageable: Pageable): Page<CompanyView> {
        return queryDsl.findAllViews(pageable)
    }

    override fun findAllActiveViews(pageable: Pageable): Page<CompanyView> {
        return queryDsl.findAllActiveViews(pageable)
    }

    override fun findAllNames(): List<CompanyNameView> {
        return queryDsl.findAllNames()
    }

}
