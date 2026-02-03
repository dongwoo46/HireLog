package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.application.view.CompanyRelationView
import com.hirelog.api.company.domain.QCompanyRelation.companyRelation
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository

@Repository
class CompanyRelationJpaQueryDslImpl(
    private val queryFactory: JPAQueryFactory
) {

    fun findAllByParentCompanyId(
        parentCompanyId: Long,
        pageable: Pageable
    ): Page<CompanyRelationView> {

        val content = queryFactory
            .select(
                Projections.constructor(
                    CompanyRelationView::class.java,
                    companyRelation.id,
                    companyRelation.parentCompanyId,
                    companyRelation.childCompanyId,
                    companyRelation.relationType,
                    companyRelation.createdAt
                )
            )
            .from(companyRelation)
            .where(companyRelation.parentCompanyId.eq(parentCompanyId))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(companyRelation.createdAt.desc())
            .fetch()

        val countQuery = queryFactory
            .select(companyRelation.count())
            .from(companyRelation)
            .where(companyRelation.parentCompanyId.eq(parentCompanyId))

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    fun findAllByChildCompanyId(
        childCompanyId: Long,
        pageable: Pageable
    ): Page<CompanyRelationView> {

        val content = queryFactory
            .select(
                Projections.constructor(
                    CompanyRelationView::class.java,
                    companyRelation.id,
                    companyRelation.parentCompanyId,
                    companyRelation.childCompanyId,
                    companyRelation.relationType,
                    companyRelation.createdAt
                )
            )
            .from(companyRelation)
            .where(companyRelation.childCompanyId.eq(childCompanyId))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(companyRelation.createdAt.desc())
            .fetch()

        val countQuery = queryFactory
            .select(companyRelation.count())
            .from(companyRelation)
            .where(companyRelation.childCompanyId.eq(childCompanyId))

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    fun findView(
        parentCompanyId: Long,
        childCompanyId: Long
    ): CompanyRelationView? {
        return queryFactory
            .select(
                Projections.constructor(
                    CompanyRelationView::class.java,
                    companyRelation.id,
                    companyRelation.parentCompanyId,
                    companyRelation.childCompanyId,
                    companyRelation.relationType,
                    companyRelation.createdAt
                )
            )
            .from(companyRelation)
            .where(
                companyRelation.parentCompanyId.eq(parentCompanyId),
                companyRelation.childCompanyId.eq(childCompanyId)
            )
            .fetchOne()
    }
}
