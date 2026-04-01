package com.hirelog.api.comment.application.port

import com.hirelog.api.comment.application.view.CommentView
import com.hirelog.api.common.application.port.PagedResult

interface CommentQuery {
    fun findByBoardId(
        boardId: Long,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<CommentView>
}
