package com.hirelog.api.member.infra.persistence.querydsl

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.application.view.MemberDetailView
import com.hirelog.api.member.application.view.MemberSummaryView
import com.hirelog.api.member.application.view.PositionView
import com.hirelog.api.member.domain.MemberStatus
import com.hirelog.api.member.domain.QMember.member
import com.hirelog.api.position.domain.QPosition.position
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class MemberJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) : MemberQuery {

    /**
     * 회원 목록 페이징 조회 (0-based page)
     */
    override fun findAllPaged(
        page: Int,
        size: Int
    ): PagedResult<MemberSummaryView> {

        val offset = page.toLong() * size

        val content = queryFactory
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
            .offset(offset)
            .limit(size.toLong())
            .fetch()

        val totalCount = queryFactory
            .select(member.count())
            .from(member)
            .fetchOne() ?: 0L

        return PagedResult.of(
            items = content,
            page = page,
            size = size,
            totalElements = totalCount
        )
    }

    /**
     * 회원 상세 조회
     */
    override fun findDetailById(id: Long): MemberDetailView? {

        return queryFactory
            .select(
                Projections.constructor(
                    MemberDetailView::class.java,
                    member.id,
                    member.email,
                    member.username,
                    member.role,
                    member.status,

                    // LEFT JOIN 결과 → PositionView nullable
                    Projections.constructor(
                        PositionView::class.java,
                        position.id,
                        position.name
                    ),

                    member.careerYears,
                    member.summary,
                    member.createdAt
                )
            )
            .from(member)
            .leftJoin(position)
            .on(member.currentPositionId.eq(position.id))
            .where(member.id.eq(id))
            .fetchOne()
    }

    /**
     * 존재 여부 확인
     */
    override fun existsById(id: Long): Boolean =
        queryFactory
            .selectOne()
            .from(member)
            .where(member.id.eq(id))
            .fetchFirst() != null

    override fun existsByIdAndStatus(
        id: Long,
        status: MemberStatus
    ): Boolean =
        queryFactory
            .selectOne()
            .from(member)
            .where(
                member.id.eq(id),
                member.status.eq(status)
            )
            .fetchFirst() != null

    override fun existsByUsername(username: String): Boolean =
        queryFactory
            .selectOne()
            .from(member)
            .where(member.username.eq(username))
            .fetchFirst() != null

    override fun existsByEmail(email: String): Boolean =
        queryFactory
            .selectOne()
            .from(member)
            .where(member.email.eq(email))
            .fetchFirst() != null

    override fun existsActiveByUsername(username: String): Boolean {
        return queryFactory
            .selectOne()
            .from(member)
            .where(
                member.username.eq(username),
                member.status.eq(MemberStatus.ACTIVE)
            )
            .fetchFirst() != null
    }

    override fun existsActiveByEmail(email: String): Boolean {
        return queryFactory
            .selectOne()
            .from(member)
            .where(
                member.email.eq(email),
                member.status.eq(MemberStatus.ACTIVE)
            )
            .fetchFirst() != null
    }

}
