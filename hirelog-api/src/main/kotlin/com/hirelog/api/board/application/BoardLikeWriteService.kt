package com.hirelog.api.board.application

import com.hirelog.api.board.application.port.BoardLikeCommand
import com.hirelog.api.common.logging.log
import com.hirelog.api.relation.domain.model.BoardLike
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BoardLikeWriteService(
    private val command: BoardLikeCommand
) {

    @Transactional
    fun like(boardId: Long, memberId: Long) {
        require(command.findByMemberIdAndBoardId(memberId, boardId) == null) {
            "이미 좋아요한 게시글입니다"
        }
        command.save(BoardLike.create(memberId = memberId, boardId = boardId))
        log.info("[BOARD_LIKED] boardId={}, memberId={}", boardId, memberId)
    }

    @Transactional
    fun unlike(boardId: Long, memberId: Long) {
        val like = command.findByMemberIdAndBoardId(memberId, boardId)
            ?: throw IllegalArgumentException("좋아요한 게시글이 아닙니다")
        command.delete(like)
        log.info("[BOARD_UNLIKED] boardId={}, memberId={}", boardId, memberId)
    }
}