package com.hirelog.api.board.application.port

import com.hirelog.api.board.application.view.BoardView
import com.hirelog.api.board.domain.BoardSortType
import com.hirelog.api.board.domain.BoardType
import com.hirelog.api.common.application.port.PagedResult

interface BoardQuery {
    fun findAll(
        boardType: BoardType?,
        memberId: Long?,
        keyword: String?,
        sortBy: BoardSortType,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<BoardView>

    fun findById(id: Long, viewerMemberId: Long): BoardView?
}
