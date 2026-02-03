package com.hirelog.api.position.infra.persistence.jpa.repository

import com.hirelog.api.position.application.view.PositionCategoryView
import com.hirelog.api.position.domain.QPositionCategory.positionCategory
import com.hirelog.api.userrequest.application.port.PagedResult
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class PositionCategoryJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    /**
     * PositionCategory 목록 조회 (페이지네이션)
     */
    fun findAllPaged(page: Int, size: Int): PagedResult<PositionCategoryView> {
        val offset = page * size

        val items = queryFactory
            .select(
                Projections.constructor(
                    PositionCategoryView::class.java,
                    positionCategory.id,
                    positionCategory.name,
                    positionCategory.normalizedName,
                    positionCategory.status,
                    positionCategory.description
                )
            )
            .from(positionCategory)
            .orderBy(positionCategory.createdAt.desc())
            .offset(offset.toLong())
            .limit(size.toLong())
            .fetch()

        val total = queryFactory
            .select(positionCategory.count())
            .from(positionCategory)
            .fetchOne() ?: 0L

        return PagedResult(
            items = items,
            page = page,
            size = size,
            totalElements = total,
            totalPages = if (size > 0) ((total + size - 1) / size).toInt() else 0,
            hasNext = offset + size < total
        )
    }

    /**
     * PositionCategory 상세 조회
     */
    fun findDetailById(id: Long): PositionCategoryView? {
        return queryFactory
            .select(
                Projections.constructor(
                    PositionCategoryView::class.java,
                    positionCategory.id,
                    positionCategory.name,
                    positionCategory.normalizedName,
                    positionCategory.status,
                    positionCategory.description
                )
            )
            .from(positionCategory)
            .where(positionCategory.id.eq(id))
            .fetchOne()
    }

    fun existsById(id: Long): Boolean =
        queryFactory
            .selectOne()
            .from(positionCategory)
            .where(positionCategory.id.eq(id))
            .fetchFirst() != null

    fun existsByNormalizedName(normalizedName: String): Boolean =
        queryFactory
            .selectOne()
            .from(positionCategory)
            .where(positionCategory.normalizedName.eq(normalizedName))
            .fetchFirst() != null
}
