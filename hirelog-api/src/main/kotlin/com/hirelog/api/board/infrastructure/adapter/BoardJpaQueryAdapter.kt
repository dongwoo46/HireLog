package com.hirelog.api.board.infrastructure.adapter

import com.hirelog.api.board.application.port.BoardQuery
import com.hirelog.api.board.application.view.BoardView
import com.hirelog.api.board.domain.BoardType
import com.hirelog.api.board.domain.QBoard
import com.hirelog.api.comment.domain.QComment
import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.member.domain.QMember
import com.hirelog.api.relation.domain.model.QBoardLike
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class BoardJpaQueryAdapter(
    private val queryFactory: JPAQueryFactory
) : BoardQuery {

    private val board = QBoard.board
    private val member = QMember.member
    private val boardLike = QBoardLike.boardLike
    private val comment = QComment.comment

    override fun findAll(
        boardType: BoardType?,
        memberId: Long?,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<BoardView> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..100) { "size must be between 1 and 100" }

        val condition = BooleanBuilder()
        if (!includeDeleted) condition.and(board.deleted.isFalse)
        boardType?.let { condition.and(board.boardType.eq(it)) }
        memberId?.let { condition.and(board.memberId.eq(it)) }

        val totalElements = queryFactory
            .select(board.id.count())
            .from(board)
            .where(condition)
            .fetchOne() ?: 0L

        val items = queryFactory
            .select(
                Projections.constructor(
                    BoardView::class.java,
                    board.id,
                    board.memberId,
                    member.username,
                    board.boardType,
                    board.title,
                    board.content,
                    board.anonymous,
                    boardLike.id.countDistinct(),
                    comment.id.countDistinct(),
                    board.deleted,
                    board.createdAt
                )
            )
            .from(board)
            .leftJoin(member).on(board.memberId.eq(member.id))
            .leftJoin(boardLike).on(boardLike.boardId.eq(board.id))
            .leftJoin(comment).on(comment.boardId.eq(board.id).and(comment.deleted.isFalse))
            .where(condition)
            .groupBy(board.id, member.username, board.boardType, board.memberId,
                board.title, board.content, board.anonymous, board.deleted, board.createdAt)
            .orderBy(board.id.desc())
            .offset((page * size).toLong())
            .limit(size.toLong())
            .fetch()

        return PagedResult.of(items = items, page = page, size = size, totalElements = totalElements)
    }

    override fun findById(id: Long, viewerMemberId: Long): BoardView? {
        return queryFactory
            .select(
                Projections.constructor(
                    BoardView::class.java,
                    board.id,
                    board.memberId,
                    member.username,
                    board.boardType,
                    board.title,
                    board.content,
                    board.anonymous,
                    boardLike.id.countDistinct(),
                    comment.id.countDistinct(),
                    board.deleted,
                    board.createdAt
                )
            )
            .from(board)
            .leftJoin(member).on(board.memberId.eq(member.id))
            .leftJoin(boardLike).on(boardLike.boardId.eq(board.id))
            .leftJoin(comment).on(comment.boardId.eq(board.id).and(comment.deleted.isFalse))
            .where(board.id.eq(id).and(board.deleted.isFalse))
            .groupBy(board.id, member.username, board.boardType, board.memberId,
                board.title, board.content, board.anonymous, board.deleted, board.createdAt)
            .fetchOne()
    }
}
