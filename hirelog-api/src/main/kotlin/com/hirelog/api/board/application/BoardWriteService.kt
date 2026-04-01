package com.hirelog.api.board.application

import com.hirelog.api.board.application.port.BoardCommand
import com.hirelog.api.board.domain.Board
import com.hirelog.api.board.domain.BoardType
import com.hirelog.api.common.logging.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BoardWriteService(
    private val command: BoardCommand
) {

    @Transactional
    fun write(
        memberId: Long?,
        boardType: BoardType,
        title: String,
        content: String,
        anonymous: Boolean
    ): Board {
        val board = Board.create(
            memberId = memberId,
            boardType = boardType,
            title = title,
            content = content,
            anonymous = anonymous
        )
        val saved = command.save(board)
        log.info("[BOARD_CREATED] id={}, memberId={}", saved.id, memberId ?: -1L)
        return saved
    }

    @Transactional
    fun update(
        boardId: Long,
        requesterId: Long,
        isAdmin: Boolean,
        title: String,
        content: String,
        anonymous: Boolean
    ) {
        val board = command.findById(boardId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다: $boardId")

        require(!board.deleted) { "삭제된 게시글은 수정할 수 없습니다" }
        require(isAdmin || board.isWrittenBy(requesterId)) { "수정 권한이 없습니다" }

        board.update(title = title, content = content, anonymous = anonymous)
        command.save(board)
        log.info("[BOARD_UPDATED] id={}, requesterId={}", boardId, requesterId)
    }

    @Transactional
    fun delete(boardId: Long, requesterId: Long, isAdmin: Boolean) {
        val board = command.findById(boardId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다: $boardId")

        require(isAdmin || board.isWrittenBy(requesterId)) { "삭제 권한이 없습니다" }

        board.softDelete()
        command.save(board)
        log.info("[BOARD_DELETED] id={}, requesterId={}", boardId, requesterId)
    }
}
