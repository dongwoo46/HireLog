package com.hirelog.api.relation.infra.persistence.jpa.repository

import com.hirelog.api.relation.application.brandposition.view.BrandPositionListView
import com.hirelog.api.relation.domain.model.QBrandPosition.brandPosition
import com.hirelog.api.relation.presentation.controller.dto.BrandPositionSearchReq
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * BrandPosition QueryDSL Repository
 *
 * 역할:
 * - BrandPosition 조회 전용
 * - View Projection 반환
 */
@Repository
class BrandPositionJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    fun search(
        condition: BrandPositionSearchReq,
        pageable: Pageable
    ): Pair<List<BrandPositionListView>, Long> {

        val where = BooleanBuilder()

        condition.brandId?.let {
            where.and(brandPosition.brandId.eq(it))
        }
        condition.displayName?.let {
            where.and(brandPosition.displayName.containsIgnoreCase(it))
        }
        condition.status?.let {
            where.and(brandPosition.status.eq(it))
        }
        condition.source?.let {
            where.and(brandPosition.source.eq(it))
        }
        condition.approved?.let { approved ->
            if (approved) {
                where.and(brandPosition.approvedAt.isNotNull)
            } else {
                where.and(brandPosition.approvedAt.isNull)
            }
        }

        val content = queryFactory
            .select(
                Projections.constructor(
                    BrandPositionListView::class.java,
                    brandPosition.id,
                    brandPosition.brandId,
                    brandPosition.positionId,
                    brandPosition.displayName,
                    brandPosition.status,
                    brandPosition.source,
                    brandPosition.approvedAt,
                    brandPosition.approvedBy
                )
            )
            .from(brandPosition)
            .where(where)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total: Long = queryFactory
            .select(brandPosition.count())
            .from(brandPosition)
            .where(where)
            .fetchOne() ?: 0L

        return content to total
    }
}
