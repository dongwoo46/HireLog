package com.hirelog.api.board.application.port

import com.hirelog.api.relation.domain.model.BoardLike

interface BoardLikeCommand {
    fun save(boardLike: BoardLike): BoardLike
    fun findByMemberIdAndBoardId(memberId: Long, boardId: Long): BoardLike?
    fun delete(boardLike: BoardLike)
}