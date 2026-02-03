package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.application.view.CompanyCandidateView
import com.hirelog.api.company.domain.CompanyCandidateStatus
import com.hirelog.api.company.domain.QCompanyCandidate.companyCandidate
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository

@Repository
class CompanyCandidateJpaQueryDslImpl(
    private val queryFactory: JPAQueryFactory
) {

    fun findViewById(id: Long): CompanyCandidateView? {
        return queryFactory
            .select(
                Projections.constructor(
                    CompanyCandidateView::class.java,
                    companyCandidate.id,
                    companyCandidate.jdSummaryId,
                    companyCandidate.brandId,
                    companyCandidate.candidateName,
                    companyCandidate.source,
                    companyCandidate.confidenceScore,
                    companyCandidate.status,
                    companyCandidate.createdAt
                )
            )
            .from(companyCandidate)
            .where(companyCandidate.id.eq(id))
            .fetchOne()
    }

    fun findAllViewsByBrandId(
        brandId: Long,
        pageable: Pageable
    ): Page<CompanyCandidateView> {

        val content = queryFactory
            .select(
                Projections.constructor(
                    CompanyCandidateView::class.java,
                    companyCandidate.id,
                    companyCandidate.jdSummaryId,
                    companyCandidate.brandId,
                    companyCandidate.candidateName,
                    companyCandidate.source,
                    companyCandidate.confidenceScore,
                    companyCandidate.status,
                    companyCandidate.createdAt
                )
            )
            .from(companyCandidate)
            .where(companyCandidate.brandId.eq(brandId))
            .orderBy(*pageable.toOrderSpecifiers())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(companyCandidate.count())
            .from(companyCandidate)
            .where(companyCandidate.brandId.eq(brandId))

        return PageableExecutionUtils.getPage(
            content,
            pageable
        ) { countQuery.fetchOne() ?: 0L }
    }

    fun findAllViewsByStatus(
        status: CompanyCandidateStatus,
        pageable: Pageable
    ): Page<CompanyCandidateView> {

        val content = queryFactory
            .select(
                Projections.constructor(
                    CompanyCandidateView::class.java,
                    companyCandidate.id,
                    companyCandidate.jdSummaryId,
                    companyCandidate.brandId,
                    companyCandidate.candidateName,
                    companyCandidate.source,
                    companyCandidate.confidenceScore,
                    companyCandidate.status,
                    companyCandidate.createdAt
                )
            )
            .from(companyCandidate)
            .where(companyCandidate.status.eq(status))
            .orderBy(*pageable.toOrderSpecifiers())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(companyCandidate.count())
            .from(companyCandidate)
            .where(companyCandidate.status.eq(status))

        return PageableExecutionUtils.getPage(
            content,
            pageable
        ) { countQuery.fetchOne() ?: 0L }
    }

    /**
     * Pageable Sort → QueryDSL OrderSpecifier 변환
     */
    private fun Pageable.toOrderSpecifiers(): Array<OrderSpecifier<*>> {
        if (this.sort.isUnsorted) {
            return arrayOf(companyCandidate.createdAt.desc())
        }

        return this.sort.toList()
            .map { order ->
                when (order.property) {
                    "createdAt" ->
                        if (order.isAscending) companyCandidate.createdAt.asc()
                        else companyCandidate.createdAt.desc()

                    "confidenceScore" ->
                        if (order.isAscending) companyCandidate.confidenceScore.asc()
                        else companyCandidate.confidenceScore.desc()

                    else -> companyCandidate.createdAt.desc()
                }
            }
            .toTypedArray()
    }
}
