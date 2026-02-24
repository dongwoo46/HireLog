package com.hirelog.api.userrequest.infra.persistence.jpa.adapter

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.application.view.UserRequestView
import com.hirelog.api.userrequest.domain.QUserRequest
import com.hirelog.api.userrequest.domain.QUserRequestComment
import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class UserRequestQueryDslAdapter(
    private val queryFactory: JPAQueryFactory
) : UserRequestQuery {

    private val ur = QUserRequest.userRequest
    private val c = QUserRequestComment.userRequestComment

    /**
     * 단건 조회 (연관 로딩 없음)
     */
    override fun findById(id: Long): UserRequest? {
        return queryFactory
            .selectFrom(ur)
            .where(ur.id.eq(id))
            .fetchOne()
    }

    /**
     * 상세 조회 (댓글 포함)
     */
    override fun findDetailById(id: Long): UserRequest? {
        return queryFactory
            .selectFrom(ur)
            .leftJoin(ur.comments, c).fetchJoin()
            .where(ur.id.eq(id))
            .distinct()
            .fetchOne()
    }

    /**
     * 특정 회원의 요청 목록 (페이징)
     */
    override fun findByMemberId(memberId: Long, page: Int, size: Int): PagedResult<UserRequestView> {
        val offset = page.toLong() * size

        val items = queryFactory
            .select(
                Projections.constructor(
                    UserRequestView::class.java,
                    ur.id,
                    ur.memberId,
                    ur.title,
                    ur.requestType,
                    ur.status,
                    ur.createdAt
                )
            )
            .from(ur)
            .where(ur.memberId.eq(memberId))
            .orderBy(ur.id.desc())
            .offset(offset)
            .limit(size.toLong())
            .fetch()

        val totalElements = queryFactory
            .select(ur.count())
            .from(ur)
            .where(ur.memberId.eq(memberId))
            .fetchOne() ?: 0L

        return PagedResult.of(
            items = items,
            page = page,
            size = size,
            totalElements = totalElements
        )
    }

    /**
     * 관리자 / 상태별 통합 페이징 조회
     */
    /**
     * 관리자 / 상태별 통합 페이징 조회
     */
    override fun findPaged(
        status: UserRequestStatus?,
        page: Int,
        size: Int
    ): PagedResult<UserRequestView> {

        val offset = page.toLong() * size

        val conditions = mutableListOf<BooleanExpression>().apply {
            if (status != null) {
                add(ur.status.eq(status))
            }
        }

        val items = queryFactory
            .select(
                Projections.constructor(
                    UserRequestView::class.java,
                    ur.id,
                    ur.memberId,
                    ur.title,
                    ur.requestType,
                    ur.status,
                    ur.createdAt
                )
            )
            .from(ur)
            .where(*conditions.toTypedArray())
            .orderBy(ur.id.desc())
            .offset(offset)
            .limit(size.toLong())
            .fetch()

        val totalElements = queryFactory
            .select(ur.count())
            .from(ur)
            .where(*conditions.toTypedArray())
            .fetchOne() ?: 0L

        return PagedResult.of(
            items = items,
            page = page,
            size = size,
            totalElements = totalElements
        )
    }

}

