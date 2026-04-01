package com.hirelog.api.comment.infrastructure.adapter

import com.hirelog.api.comment.application.port.CommentQuery
import com.hirelog.api.comment.application.view.CommentView
import com.hirelog.api.comment.domain.QComment
import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.member.domain.QMember
import com.hirelog.api.relation.domain.model.QCommentLike
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class CommentJpaQueryAdapter(
    private val queryFactory: JPAQueryFactory
) : CommentQuery {

    private val comment = QComment.comment
    private val member = QMember.member
    private val commentLike = QCommentLike.commentLike

    override fun findByBoardId(
        boardId: Long,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<CommentView> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..100) { "size must be between 1 and 100" }

        val condition = BooleanBuilder()
        condition.and(comment.boardId.eq(boardId))
        if (!includeDeleted) condition.and(comment.deleted.isFalse)

        val totalElements = queryFactory
            .select(comment.id.count())
            .from(comment)
            .where(condition)
            .fetchOne() ?: 0L

        val items = queryFactory
            .select(
                Projections.constructor(
                    CommentView::class.java,
                    comment.id,
                    comment.boardId,
                    comment.memberId,
                    member.username,
                    comment.content,
                    comment.anonymous,
                    commentLike.id.countDistinct(),
                    comment.deleted,
                    comment.createdAt
                )
            )
            .from(comment)
            .leftJoin(member).on(comment.memberId.eq(member.id))
            .leftJoin(commentLike).on(commentLike.commentId.eq(comment.id))
            .where(condition)
            .groupBy(comment.id, comment.boardId, comment.memberId, member.username,
                comment.content, comment.anonymous, comment.deleted, comment.createdAt)
            .orderBy(comment.id.asc())
            .offset((page * size).toLong())
            .limit(size.toLong())
            .fetch()

        return PagedResult.of(items = items, page = page, size = size, totalElements = totalElements)
    }
}