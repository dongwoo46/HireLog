package com.hirelog.api.position.infra.persistence.jpa.repository

import com.hirelog.api.position.application.view.PositionCategoryView
import com.hirelog.api.position.application.view.PositionDetailView
import com.hirelog.api.position.application.view.PositionSummaryView
import com.hirelog.api.position.domain.QPosition.position
import com.hirelog.api.position.domain.QPositionCategory.positionCategory
import com.hirelog.api.userrequest.application.port.PagedResult
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class PositionJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    /**
     * Position 목록 조회 (페이지네이션)
     * - Category join
     */
    fun findAllPaged(page: Int, size: Int): PagedResult<PositionSummaryView> {
        val offset = page * size

        val items = queryFactory
            .select(
                Projections.constructor(
                    PositionSummaryView::class.java,
                    position.id,
                    position.name,
                    position.status,
                    positionCategory.id,
                    positionCategory.name
                )
            )
            .from(position)
            .join(position.category, positionCategory)
            .orderBy(position.createdAt.desc())
            .offset(offset.toLong())
            .limit(size.toLong())
            .fetch()

        val total = queryFactory
            .select(position.count())
            .from(position)
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
     * Position 상세 조회
     * - Category join
     */
    fun findDetailById(id: Long): PositionDetailView? {
        return queryFactory
            .select(
                Projections.constructor(
                    PositionDetailView::class.java,
                    position.id,
                    position.name,
                    position.normalizedName,
                    position.status,
                    position.description,
                    position.createdAt,
                    Projections.constructor(
                        PositionCategoryView::class.java,
                        positionCategory.id,
                        positionCategory.name,
                        positionCategory.normalizedName,
                        positionCategory.status,
                        positionCategory.description
                    )
                )
            )
            .from(position)
            .join(position.category, positionCategory)
            .where(position.id.eq(id))
            .fetchOne()
    }

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

    /**
     * normalizedName으로 Position 조회
     */
    fun findByNormalizedName(normalizedName: String): PositionSummaryView? {
        return queryFactory
            .select(
                Projections.constructor(
                    PositionSummaryView::class.java,
                    position.id,
                    position.name,
                    position.status,
                    positionCategory.id,
                    positionCategory.name
                )
            )
            .from(position)
            .join(position.category, positionCategory)
            .where(position.normalizedName.eq(normalizedName))
            .fetchOne()
    }

    /**
     * 활성 Position 이름 목록 조회 (LLM용)
     */
    fun findActiveNames(): List<String> {
        return queryFactory
            .select(position.name)
            .from(position)
            .where(position.status.eq(com.hirelog.api.position.domain.PositionStatus.ACTIVE))
            .fetch()
    }
}
