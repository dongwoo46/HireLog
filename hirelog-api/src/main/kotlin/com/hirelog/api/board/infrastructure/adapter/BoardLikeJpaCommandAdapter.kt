package com.hirelog.api.board.infrastructure.adapter

import com.hirelog.api.board.application.port.BoardLikeCommand
import com.hirelog.api.board.infrastructure.BoardLikeJpaRepository
import com.hirelog.api.relation.domain.model.BoardLike
import org.springframework.stereotype.Component

@Component
class BoardLikeJpaCommandAdapter(
    private val repository: BoardLikeJpaRepository
) : BoardLikeCommand {

    override fun save(boardLike: BoardLike): BoardLike = repository.save(boardLike)

    override fun findByMemberIdAndBoardId(memberId: Long, boardId: Long): BoardLike? =
        repository.findByMemberIdAndBoardId(memberId, boardId)

    override fun delete(boardLike: BoardLike) = repository.delete(boardLike)
}
