package com.hirelog.api.brand.infra.persistence.jpa.repository

import com.hirelog.api.brand.application.query.BrandQuery
import com.hirelog.api.brand.application.view.BrandDetailView
import com.hirelog.api.brand.application.view.BrandSummaryView
import com.hirelog.api.brand.application.view.CompanyView
import com.hirelog.api.brand.domain.QBrand.brand
import com.hirelog.api.company.domain.QCompany.company
import com.hirelog.api.userrequest.application.port.PagedResult
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class BrandJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    /**
     * 브랜드 목록 조회 (페이지네이션)
     * - 리스트용 최소 컬럼만 Projection
     * - Company join ❌
     */
    fun findAllPaged(
        page: Int,
        size: Int
    ): PagedResult<BrandSummaryView> {

        val offset = page * size

        val items = queryFactory
            .select(
                Projections.constructor(
                    BrandSummaryView::class.java,
                    brand.id,
                    brand.name,
                    brand.verificationStatus,
                    brand.isActive
                )
            )
            .from(brand)
            .orderBy(brand.createdAt.desc())
            .offset(offset.toLong())
            .limit(size.toLong())
            .fetch()

        val total = queryFactory
            .select(brand.count())
            .from(brand)
            .fetchOne() ?: 0L

        return PagedResult(
            items = items,
            page = page,
            size = size,
            totalElements = total,
            totalPages = ((total + size - 1) / size).toInt(),
            hasNext = offset + size < total
        )
    }

    /**
     * 브랜드 상세 조회
     * - Company 존재 시에만 join
     * - 단건 조회
     */
    fun findDetailById(brandId: Long): BrandDetailView? {

        val result = queryFactory
            .select(
                Projections.constructor(
                    BrandDetailView::class.java,
                    brand.id,
                    brand.name,
                    brand.normalizedName,
                    brand.verificationStatus,
                    brand.source,
                    brand.isActive,
                    brand.createdAt,
                    Projections.constructor(
                        CompanyView::class.java,
                        company.id,
                        company.name,
                        company.normalizedName,
                        company.verificationStatus,
                        company.isActive
                    )
                )
            )
            .from(brand)
            .leftJoin(company)
            .on(brand.companyId.eq(company.id))
            .where(brand.id.eq(brandId))
            .fetchOne()

        return result
    }

    /**
     * Write 선행 검증
     * - index only scan
     */
    fun existsById(brandId: Long): Boolean =
        queryFactory
            .selectOne()
            .from(brand)
            .where(brand.id.eq(brandId))
            .fetchFirst() != null

    /**
     * normalizedName 중복 검증
     */
    fun existsByNormalizedName(normalizedName: String): Boolean =
        queryFactory
            .selectOne()
            .from(brand)
            .where(brand.normalizedName.eq(normalizedName))
            .fetchFirst() != null
}
