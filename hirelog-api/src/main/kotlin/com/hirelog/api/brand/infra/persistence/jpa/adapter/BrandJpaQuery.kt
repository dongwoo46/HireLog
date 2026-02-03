package com.hirelog.api.brand.infra.persistence.jpa.adapter

import com.hirelog.api.brand.application.query.BrandQuery
import com.hirelog.api.brand.application.view.BrandDetailView
import com.hirelog.api.brand.application.view.BrandSummaryView
import com.hirelog.api.brand.infra.persistence.jpa.repository.BrandJpaQueryDsl
import com.hirelog.api.userrequest.application.port.PagedResult
import org.springframework.stereotype.Component

/**
 * BrandJpaQueryAdapter
 *
 * 책임:
 * - BrandQuery 포트 구현
 * - QueryDSL 구현체로 위임
 *
 * 주의:
 * - 정책 ❌
 * - 검증 로직 ❌
 * - 가공 ❌
 */
@Component
class BrandJpaQuery(
    private val queryDsl: BrandJpaQueryDsl
) : BrandQuery {

    override fun findAllPaged(
        page: Int,
        size: Int
    ): PagedResult<BrandSummaryView> {
        return queryDsl.findAllPaged(page, size)
    }

    override fun findDetailById(brandId: Long): BrandDetailView? {
        return queryDsl.findDetailById(brandId)
    }

    override fun existsById(brandId: Long): Boolean {
        return queryDsl.existsById(brandId)
    }

    override fun existsByNormalizedName(normalizedName: String): Boolean {
        return queryDsl.existsByNormalizedName(normalizedName)
    }
}
