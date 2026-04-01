package com.hirelog.api.board.application

import com.hirelog.api.board.application.port.BoardLikeQuery
import com.hirelog.api.board.application.view.BoardLikeStat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BoardLikeReadService(
    private val query: BoardLikeQuery
) {

    @Transactional(readOnly = true)
    fun getStat(boardId: Long, memberId: Long): BoardLikeStat {
        return query.getStat(boardId = boardId, memberId = memberId)
    }
}