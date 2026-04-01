package com.hirelog.api.board.infrastructure.adapter

import com.hirelog.api.board.application.port.BoardLikeQuery
import com.hirelog.api.board.application.view.BoardLikeStat
import com.hirelog.api.board.infrastructure.BoardLikeJpaRepository
import org.springframework.stereotype.Component

@Component
class BoardLikeJpaQueryAdapter(
    private val repository: BoardLikeJpaRepository
) : BoardLikeQuery {

    override fun getStat(boardId: Long, memberId: Long): BoardLikeStat {
        return BoardLikeStat(
            likeCount = repository.countByBoardId(boardId),
            liked = repository.existsByMemberIdAndBoardId(memberId, boardId)
        )
    }
}
