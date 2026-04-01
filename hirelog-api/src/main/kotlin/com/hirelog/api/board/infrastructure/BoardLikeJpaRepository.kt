package com.hirelog.api.board.infrastructure

import com.hirelog.api.relation.domain.model.BoardLike
import org.springframework.data.jpa.repository.JpaRepository

interface BoardLikeJpaRepository : JpaRepository<BoardLike, Long> {
    fun findByMemberIdAndBoardId(memberId: Long, boardId: Long): BoardLike?
    fun countByBoardId(boardId: Long): Long
    fun existsByMemberIdAndBoardId(memberId: Long, boardId: Long): Boolean
}