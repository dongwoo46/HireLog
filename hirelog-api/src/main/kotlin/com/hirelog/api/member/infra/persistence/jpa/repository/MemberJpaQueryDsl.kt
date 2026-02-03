package com.hirelog.api.member.infra.persistence.jpa.repository

import com.hirelog.api.member.application.view.MemberDetailView
import com.hirelog.api.member.application.view.MemberSummaryView
import com.hirelog.api.member.domain.MemberStatus
import com.hirelog.api.member.domain.QMember.member
import com.hirelog.api.userrequest.application.port.PagedResult
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class MemberJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    /**
     * Member 목록 조회 (페이지네이션)
     */
    fun findAllPaged(page: Int, size: Int): PagedResult<MemberSummaryView> {
        val offset = page * size

        val items = queryFactory
            .select(
                Projections.constructor(
                    MemberSummaryView::class.java,
                    member.id,
                    member.email,
                    member.username,
                    member.role,
                    member.status
                )
            )
            .from(member)
            .orderBy(member.createdAt.desc())
            .offset(offset.toLong())
            .limit(size.toLong())
            .fetch()

        val total = queryFactory
            .select(member.count())
            .from(member)
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
     * Member 상세 조회
     */
    fun findDetailById(id: Long): MemberDetailView? {
        return queryFactory
            .select(
                Projections.constructor(
                    MemberDetailView::class.java,
                    member.id,
                    member.email,
                    member.username,
                    member.role,
                    member.status,
                    member.currentPositionId,
                    member.careerYears,
                    member.summary,
                    member.createdAt
                )
            )
            .from(member)
            .where(member.id.eq(id))
            .fetchOne()
    }

    fun existsById(id: Long): Boolean =
        queryFactory
            .selectOne()
            .from(member)
            .where(member.id.eq(id))
            .fetchFirst() != null

    fun existsByIdAndStatus(id: Long, status: MemberStatus): Boolean =
        queryFactory
            .selectOne()
            .from(member)
            .where(
                member.id.eq(id),
                member.status.eq(status)
            )
            .fetchFirst() != null

    fun existsByUsername(username: String): Boolean =
        queryFactory
            .selectOne()
            .from(member)
            .where(member.username.eq(username))
            .fetchFirst() != null

    fun existsByEmail(email: String): Boolean =
        queryFactory
            .selectOne()
            .from(member)
            .where(member.email.eq(email))
            .fetchFirst() != null
}
