package com.hirelog.api.position.infra.persistence.jpa.repository

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.position.application.view.PositionCategoryView
import com.hirelog.api.position.application.view.PositionDetailView
import com.hirelog.api.position.application.view.PositionListView
import com.hirelog.api.position.application.view.PositionView
import com.hirelog.api.position.domain.PositionStatus
import com.hirelog.api.position.domain.QPosition.position
import com.hirelog.api.position.domain.QPositionCategory.positionCategory
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * PositionJpaQueryDsl
 *
 * 책임:
 * - QueryDSL 기반 실제 조회 로직
 * - Port / Adapter 개념 ❌
 */
@Component
class PositionJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    fun findAll(
        status: String?,
        categoryId: Long?,
        name: String?,
        pageable: Pageable
    ): PagedResult<PositionListView> {

        val conditions = mutableListOf<BooleanExpression>()

        status?.let {
            conditions += position.status.eq(PositionStatus.valueOf(it))
        }

        categoryId?.let {
            conditions += position.category.id.eq(it)
        }

        name?.let {
            conditions += position.name.containsIgnoreCase(it)
        }

        val items = queryFactory
            .select(
                Projections.constructor(
                    PositionListView::class.java,
                    position.id,
                    position.name,
                    position.status.stringValue()
                )
            )
            .from(position)
            .where(*conditions.toTypedArray())
            .orderBy(position.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(position.count())
            .from(position)
            .where(*conditions.toTypedArray())
            .fetchOne() ?: 0L

        return PagedResult.of(
            items = items,
            page = pageable.pageNumber,
            size = pageable.pageSize,
            totalElements = total
        )
    }

    fun findDetailById(id: Long): PositionDetailView? =
        queryFactory
            .select(
                Projections.constructor(
                    PositionDetailView::class.java,
                    position.id,
                    position.name,
                    position.normalizedName,
                    position.status.stringValue(),
                    position.description,
                    Projections.constructor(
                        PositionCategoryView::class.java,
                        positionCategory.id,
                        positionCategory.name,
                        positionCategory.description
                    )
                )
            )
            .from(position)
            .join(position.category, positionCategory)
            .where(position.id.eq(id))
            .fetchOne()

    fun findByNormalizedName(normalizedName: String): PositionView? =
        queryFactory
            .select(
                Projections.constructor(
                    PositionView::class.java,
                    position.id,
                    position.name,
                    position.status.stringValue(),
                    Projections.constructor(
                        PositionCategoryView::class.java,
                        positionCategory.id,
                        positionCategory.name
                    )
                )
            )
            .from(position)
            .join(position.category, positionCategory)
            .where(position.normalizedName.eq(normalizedName))
            .fetchOne()

    fun findActiveNames(): List<String> =
        queryFactory
            .select(position.name)
            .from(position)
            .where(position.status.eq(PositionStatus.ACTIVE))
            .fetch()

    fun existsById(id: Long): Boolean =
        queryFactory
            .selectOne()
            .from(position)
            .where(position.id.eq(id))
            .fetchFirst() != null

    fun existsByNormalizedName(normalizedName: String): Boolean =
        queryFactory
            .selectOne()
            .from(position)
            .where(position.normalizedName.eq(normalizedName))
            .fetchFirst() != null
}
