package com.hirelog.api.board.application

import com.hirelog.api.board.application.port.BoardQuery
import com.hirelog.api.board.application.view.BoardView
import com.hirelog.api.board.domain.BoardSortType
import com.hirelog.api.board.domain.BoardType
import com.hirelog.api.common.application.port.PagedResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BoardReadService(
    private val query: BoardQuery
) {

    @Transactional(readOnly = true)
    fun findAll(
        boardType: BoardType?,
        memberId: Long?,
        keyword: String?,
        sortBy: BoardSortType,
        deleted: Boolean?,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<BoardView> {
        return query.findAll(
            boardType = boardType,
            memberId = memberId,
            keyword = keyword,
            sortBy = sortBy,
            deleted = deleted,
            includeDeleted = includeDeleted,
            page = page,
            size = size
        )
    }

    @Transactional(readOnly = true)
    fun findById(id: Long, viewerMemberId: Long): BoardView {
        return query.findById(id = id, viewerMemberId = viewerMemberId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다: $id")
    }
}
