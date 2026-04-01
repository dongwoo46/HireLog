package com.hirelog.api.comment.application

import com.hirelog.api.comment.application.port.CommentQuery
import com.hirelog.api.comment.application.view.CommentView
import com.hirelog.api.common.application.port.PagedResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentReadService(
    private val query: CommentQuery
) {

    @Transactional(readOnly = true)
    fun findByBoardId(
        boardId: Long,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<CommentView> {
        return query.findByBoardId(
            boardId = boardId,
            includeDeleted = includeDeleted,
            page = page,
            size = size
        )
    }
}