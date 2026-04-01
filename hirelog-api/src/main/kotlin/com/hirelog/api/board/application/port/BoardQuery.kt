package com.hirelog.api.board.application.port

import com.hirelog.api.board.application.view.BoardView
import com.hirelog.api.board.domain.BoardType
import com.hirelog.api.common.application.port.PagedResult

interface BoardQuery {
    fun findAll(
        boardType: BoardType?,
        memberId: Long?,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<BoardView>

    fun findById(id: Long, viewerMemberId: Long): BoardView?
}