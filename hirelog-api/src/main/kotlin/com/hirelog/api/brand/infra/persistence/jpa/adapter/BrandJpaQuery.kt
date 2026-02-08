package com.hirelog.api.brand.infra.persistence.jpa.adapter

import com.hirelog.api.brand.application.port.BrandQuery
import com.hirelog.api.brand.application.view.BrandDetailView
import com.hirelog.api.brand.application.view.BrandListView
import com.hirelog.api.brand.infrastructure.querydsl.BrandJpaQueryDsl
import com.hirelog.api.brand.infra.persistence.jpa.repository.BrandJpaRepository
import com.hirelog.api.brand.presentation.controller.dto.BrandSearchReq
import com.hirelog.api.common.application.port.PagedResult
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * BrandJpaQueryAdapter
 *
 * 책임:
 * - BrandQuery 포트 구현
 * - QueryDSL + JPA Repository 조합
 */
@Component
class BrandJpaQuery(
    private val queryDsl: BrandJpaQueryDsl,
    private val repository: BrandJpaRepository
) : BrandQuery {

    override fun search(
        condition: BrandSearchReq,
        pageable: Pageable
    ): PagedResult<BrandListView> {

        val (content, total) = queryDsl.search(condition, pageable)

        return PagedResult.of(
            items = content,
            page = pageable.pageNumber,
            size = pageable.pageSize,
            totalElements = total
        )
    }


    override fun findDetailById(brandId: Long): BrandDetailView? {
        return queryDsl.findDetailById(brandId)
    }

    /**
     * Write 선행 검증
     */
    override fun existsById(brandId: Long): Boolean {
        return repository.existsById(brandId)
    }

    override fun existsByNormalizedName(normalizedName: String): Boolean {
        return repository.existsByNormalizedName(normalizedName)
    }
}
