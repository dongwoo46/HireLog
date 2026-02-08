package com.hirelog.api.position.infra.persistence.jpa.repository

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.position.application.view.PositionCategoryDetailView
import com.hirelog.api.position.application.view.PositionCategoryListView
import com.hirelog.api.position.domain.PositionStatus
import com.hirelog.api.position.domain.QPositionCategory.positionCategory
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class PositionCategoryJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    fun findAll(
        status: PositionStatus?,
        name: String?,
        pageable: Pageable
    ): PagedResult<PositionCategoryListView> {

        val items = queryFactory
            .select(
                Projections.constructor(
                    PositionCategoryListView::class.java,
                    positionCategory.id,
                    positionCategory.name,
                    positionCategory.status.stringValue() // ✅ 핵심
                )
            )
            .from(positionCategory)
            .where(
                status?.let { positionCategory.status.eq(it) },
                name?.let { positionCategory.name.containsIgnoreCase(it) }
            )
            .orderBy(positionCategory.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(positionCategory.count())
            .from(positionCategory)
            .where(
                status?.let { positionCategory.status.eq(it) },
                name?.let { positionCategory.name.containsIgnoreCase(it) }
            )
            .fetchOne() ?: 0L

        return PagedResult.of(
            items = items,
            page = pageable.pageNumber,
            size = pageable.pageSize,
            totalElements = total
        )
    }

    fun findDetailById(id: Long): PositionCategoryDetailView? =
        queryFactory
            .select(
                Projections.constructor(
                    PositionCategoryDetailView::class.java,
                    positionCategory.id,
                    positionCategory.name,
                    positionCategory.normalizedName,
                    positionCategory.status.stringValue(),
                    positionCategory.description
                )
            )
            .from(positionCategory)
            .where(positionCategory.id.eq(id))
            .fetchOne()

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
