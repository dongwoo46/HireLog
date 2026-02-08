package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.application.view.CompanyDetailView
import com.hirelog.api.company.application.view.CompanyNameView
import com.hirelog.api.company.application.view.CompanyView
import com.hirelog.api.company.presentation.controller.dto.CompanySearchReq
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyJpaQueryDsl
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * CompanyJpaQueryAdapter
 *
 * 책임:
 * - CompanyQuery 포트 구현
 * - 조회 / 존재 여부 확인 위임
 *
 * 원칙:
 * - Port에 정의된 메서드만 구현
 * - 비즈니스 판단 ❌
 * - 단순 조회 위임만 수행
 */
@Component
class CompanyJpaQuery(
    private val queryDsl: CompanyJpaQueryDsl,
    private val repository: CompanyJpaRepository
) : CompanyQuery {

    override fun existsByNormalizedName(normalizedName: String): Boolean {
        return repository.existsByNormalizedName(normalizedName)
    }

    override fun findViewById(companyId: Long): CompanyView? {
        return queryDsl.findViewById(companyId)
    }

    override fun findDetailById(companyId: Long): CompanyDetailView? {
        return queryDsl.findDetailById(companyId)
    }

    override fun search(
        condition: CompanySearchReq,
        pageable: Pageable
    ): Page<CompanyView> {
        return queryDsl.search(condition, pageable)
    }

    // Company 전체 이름 목록 조회
    override fun findAllNames(): List<CompanyNameView> {
        return queryDsl.findAllNames()
    }
}
