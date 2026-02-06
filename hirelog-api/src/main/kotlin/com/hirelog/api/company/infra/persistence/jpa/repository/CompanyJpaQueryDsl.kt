package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.application.view.BrandView
import com.hirelog.api.company.application.view.CompanyDetailView
import com.hirelog.api.company.application.view.CompanyView
import com.hirelog.api.company.presentation.controller.dto.CompanySearchReq
import com.hirelog.api.company.domain.QCompany.company
import com.hirelog.api.brand.domain.QBrand.brand
import com.hirelog.api.company.application.view.CompanyNameView
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository

@Repository
class CompanyJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    /**
     * Company 단건 조회 (View)
     */
    fun findViewById(companyId: Long): CompanyView? {
        return queryFactory
            .select(
                Projections.constructor(
                    CompanyView::class.java,
                    company.id,
                    company.name,
                    company.source,
                    company.isActive
                )
            )
            .from(company)
            .where(company.id.eq(companyId))
            .fetchOne()
    }

    /**
     * Company 상세 조회
     *
     * 의미:
     * - Company + 소속 Brand 목록
     */
    fun findDetailById(companyId: Long): CompanyDetailView? {

        val companyView  = queryFactory
            .select(
                Projections.constructor(
                    CompanyDetailView::class.java,
                    company.id,
                    company.name,
                    company.source.stringValue(),
                    company.isActive,
                    Expressions.constant(emptyList<BrandView>())
                )
            )
            .from(company)
            .where(company.id.eq(companyId))
            .fetchOne()
            ?: return null


        val brands = queryFactory
            .select(
                Projections.constructor(
                    BrandView::class.java,
                    brand.id,
                    brand.name,
                    brand.verificationStatus.stringValue(),
                    brand.isActive
                )
            )
            .from(brand)
            .where(
                brand.companyId.eq(companyId),
                brand.isActive.isTrue
            )
            .orderBy(brand.id.asc())
            .fetch()

        return companyView.copy(brands = brands)
    }

    /**
     * Company 검색
     *
     * 조건:
     * - name (LIKE)
     * - isActive
     */
    fun search(
        condition: CompanySearchReq,
        pageable: Pageable
    ): Page<CompanyView> {

        val where = BooleanBuilder()

        condition.name?.let {
            where.and(company.name.containsIgnoreCase(it))
        }

        condition.isActive?.let {
            where.and(company.isActive.eq(it))
        }

        val content = queryFactory
            .select(
                Projections.constructor(
                    CompanyView::class.java,
                    company.id,
                    company.name,
                    company.source,
                    company.isActive
                )
            )
            .from(company)
            .where(where)
            .orderBy(company.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(company.count())
            .from(company)
            .where(where)

        return PageableExecutionUtils.getPage(
            content,
            pageable
        ) { countQuery.fetchOne() ?: 0L }
    }

    fun findAllNames(): List<CompanyNameView> {
        return queryFactory
            .select(
                Projections.constructor(
                    CompanyNameView::class.java,
                    company.id,
                    company.name
                )
            )
            .from(company)
            .orderBy(company.name.asc())
            .fetch()
    }

}
