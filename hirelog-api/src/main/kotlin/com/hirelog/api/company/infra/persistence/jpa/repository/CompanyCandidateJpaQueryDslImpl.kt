package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.application.view.CompanyCandidateDetailView
import com.hirelog.api.company.application.view.CompanyCandidateListView
import com.hirelog.api.company.application.view.BrandSimpleView
import com.hirelog.api.company.presentation.controller.dto.CompanyCandidateSearchReq
import com.hirelog.api.company.domain.QCompanyCandidate.companyCandidate
import com.hirelog.api.brand.domain.QBrand.brand
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.Tuple
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository

@Repository
class CompanyCandidateJpaQueryDslImpl(
    private val queryFactory: JPAQueryFactory
) {

    fun search(
        condition: CompanyCandidateSearchReq,
        pageable: Pageable
    ): Page<CompanyCandidateListView> {

        val where = BooleanBuilder()

        condition.candidateName?.let {
            where.and(companyCandidate.candidateName.containsIgnoreCase(it))
        }

        condition.source?.let {
            where.and(companyCandidate.source.eq(it))
        }

        condition.status?.let {
            where.and(companyCandidate.status.eq(it))
        }

        condition.minConfidenceScore?.let {
            where.and(companyCandidate.confidenceScore.goe(it))
        }

        condition.maxConfidenceScore?.let {
            where.and(companyCandidate.confidenceScore.loe(it))
        }

        val content = queryFactory
            .select(
                Projections.constructor(
                    CompanyCandidateListView::class.java,
                    companyCandidate.id,
                    companyCandidate.candidateName,
                    companyCandidate.source,
                    companyCandidate.confidenceScore,
                    companyCandidate.status,
                    companyCandidate.createdAt
                )
            )
            .from(companyCandidate)
            .where(where)
            .orderBy(companyCandidate.confidenceScore.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(companyCandidate.count())
            .from(companyCandidate)
            .where(where)

        return PageableExecutionUtils.getPage(
            content,
            pageable
        ) { countQuery.fetchOne() ?: 0L }
    }

    fun findDetailById(candidateId: Long): CompanyCandidateDetailView? {

        val tuple = queryFactory
            .select(
                companyCandidate.id,
                companyCandidate.candidateName,
                companyCandidate.normalizedName,
                companyCandidate.source,
                companyCandidate.confidenceScore,
                companyCandidate.status,

                // Brand 관련 컬럼들 (LEFT JOIN이므로 nullable)
                brand.id,
                brand.name,
                brand.isActive,

                companyCandidate.jdSummaryId,
                companyCandidate.createdAt
            )
            .from(companyCandidate)
            .leftJoin(brand).on(brand.id.eq(companyCandidate.brandId))
            .where(companyCandidate.id.eq(candidateId))
            .fetchOne()

        return tuple?.let { mapToDetailView(it) }
    }

    private fun mapToDetailView(tuple: Tuple): CompanyCandidateDetailView {

        val brandId = tuple[brand.id]

        val brandView: BrandSimpleView? =
            if (brandId != null) {
                BrandSimpleView(
                    id = brandId,
                    name = tuple[brand.name]!!,
                    isActive = tuple[brand.isActive]!!
                )
            } else {
                null
            }

        return CompanyCandidateDetailView(
            id = tuple[companyCandidate.id]!!,
            candidateName = tuple[companyCandidate.candidateName]!!,
            normalizedName = tuple[companyCandidate.normalizedName]!!,
            source = tuple[companyCandidate.source]!!,
            confidenceScore = tuple[companyCandidate.confidenceScore]!!,
            status = tuple[companyCandidate.status]!!,
            brand = brandView,
            jdSummaryId = tuple[companyCandidate.jdSummaryId]!!,
            createdAt = tuple[companyCandidate.createdAt]!!
        )
    }

}
