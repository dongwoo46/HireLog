package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.application.view.CompanyNameView
import com.hirelog.api.company.application.view.CompanyView
import com.hirelog.api.company.domain.QCompany.company
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository

@Repository
class CompanyJpaQueryDslImpl(
    private val queryFactory: JPAQueryFactory
) {

    fun findViewById(id: Long): CompanyView? {
        return queryFactory
            .select(
                Projections.constructor(
                    CompanyView::class.java,
                    company.id,
                    company.name,
                    company.source,
                    company.verificationStatus,
                    company.isActive,
                    company.createdAt
                )
            )
            .from(company)
            .where(company.id.eq(id))
            .fetchOne()
    }

    fun findAllViews(pageable: Pageable): Page<CompanyView> {
        val content = queryFactory
            .select(
                Projections.constructor(
                    CompanyView::class.java,
                    company.id,
                    company.name,
                    company.source,
                    company.verificationStatus,
                    company.isActive,
                    company.createdAt
                )
            )
            .from(company)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(company.createdAt.desc())
            .fetch()

        val countQuery = queryFactory
            .select(company.count())
            .from(company)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    fun findAllActiveViews(pageable: Pageable): Page<CompanyView> {
        val content = queryFactory
            .select(
                Projections.constructor(
                    CompanyView::class.java,
                    company.id,
                    company.name,
                    company.source,
                    company.verificationStatus,
                    company.isActive,
                    company.createdAt
                )
            )
            .from(company)
            .where(company.isActive.isTrue)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(company.createdAt.desc())
            .fetch()

        val countQuery = queryFactory
            .select(company.count())
            .from(company)
            .where(company.isActive.isTrue)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
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
