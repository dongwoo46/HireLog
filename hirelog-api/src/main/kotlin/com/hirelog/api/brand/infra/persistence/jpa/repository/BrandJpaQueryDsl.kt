package com.hirelog.api.brand.infrastructure.querydsl

import com.hirelog.api.brand.application.view.*
import com.hirelog.api.brand.domain.QBrand.brand
import com.hirelog.api.company.domain.QCompany.company
import com.hirelog.api.position.domain.QPosition.position
import com.hirelog.api.brand.presentation.controller.dto.BrandSearchReq
import com.hirelog.api.relation.domain.model.QBrandPosition.brandPosition
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.Tuple
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class BrandJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    fun search(
        condition: BrandSearchReq,
        pageable: Pageable
    ): Pair<List<BrandListView>, Long> {

        val where = BooleanBuilder()

        condition.isActive?.let { where.and(brand.isActive.eq(it)) }
        condition.verificationStatus?.let { where.and(brand.verificationStatus.eq(it)) }
        condition.source?.let { where.and(brand.source.eq(it)) }
        condition.name?.takeIf { it.isNotBlank() }
            ?.let { where.and(brand.name.containsIgnoreCase(it)) }

        condition.createdFrom?.let { where.and(brand.createdAt.goe(it)) }
        condition.createdTo?.let { where.and(brand.createdAt.lt(it)) }

        val content = queryFactory
            .select(
                Projections.constructor(
                    BrandListView::class.java,
                    brand.id,
                    brand.name,
                    brand.verificationStatus,
                    brand.source,
                    brand.isActive
                )
            )
            .from(brand)
            .where(where)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(brand.createdAt.desc())
            .fetch()

        val total = queryFactory
            .select(brand.count())
            .from(brand)
            .where(where)
            .fetchOne() ?: 0L

        return content to total
    }

    fun findDetailById(brandId: Long): BrandDetailView? {

        val brandTuple = queryFactory
            .select(
                brand.id,
                brand.name,
                brand.normalizedName,
                brand.verificationStatus,
                brand.source,
                brand.isActive,
                brand.createdAt,
                company.id,
                company.name,
                company.isActive
            )
            .from(brand)
            .leftJoin(company).on(company.id.eq(brand.companyId))
            .where(brand.id.eq(brandId))
            .fetchOne()
            ?: return null

        val brandPositionsTuple = queryFactory
            .select(
                brandPosition.id,
                brandPosition.positionId,
                brandPosition.displayName,
                brandPosition.status,
                position.name
            )
            .from(brandPosition)
            .join(position).on(position.id.eq(brandPosition.positionId))
            .where(brandPosition.brandId.eq(brandId))
            .orderBy(brandPosition.status.asc(), brandPosition.id.asc())
            .fetch()

        return mapToBrandDetailView(brandTuple, brandPositionsTuple)
    }

    private fun mapToBrandDetailView(
        brandTuple: Tuple,
        brandPositionTuples: List<Tuple>
    ): BrandDetailView {

        val companyView =
            brandTuple[company.id]?.let {
                CompanySimpleView(
                    id = it,
                    name = brandTuple[company.name]!!,
                    isActive = brandTuple[company.isActive]!!
                )
            }

        val brandPositions = brandPositionTuples.map {
            val displayName =
                it[brandPosition.displayName] ?: it[position.name]!!

            BrandPositionView(
                id = it[brandPosition.id]!!,
                positionId = it[brandPosition.positionId]!!,
                displayName = displayName,
                status = it[brandPosition.status]!!
            )
        }

        return BrandDetailView(
            id = brandTuple[brand.id]!!,
            name = brandTuple[brand.name]!!,
            normalizedName = brandTuple[brand.normalizedName]!!,
            company = companyView,
            verificationStatus = brandTuple[brand.verificationStatus]!!,
            source = brandTuple[brand.source]!!,
            isActive = brandTuple[brand.isActive]!!,
            createdAt = brandTuple[brand.createdAt]!!,
            brandPositions = brandPositions
        )
    }
}
