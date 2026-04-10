package com.hirelog.api.board.application

import com.hirelog.api.board.application.port.BoardCommand
import com.hirelog.api.board.domain.Board
import com.hirelog.api.board.domain.BoardType
import com.hirelog.api.common.logging.log
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BoardWriteService(
    private val command: BoardCommand
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    @Transactional
    fun write(
        memberId: Long?,
        isAdmin: Boolean,
        boardType: BoardType,
        title: String,
        content: String,
        anonymous: Boolean,
        guestPassword: String?,
        notice: Boolean,
        pinned: Boolean
    ): Board {
        val guestPasswordHash = if (memberId == null) {
            val raw = guestPassword?.trim()
            require(!raw.isNullOrBlank()) { "비로그인 작성은 비밀번호를 입력해야 합니다." }
            passwordEncoder.encode(raw)
        } else {
            null
        }

        require(!notice || isAdmin) { "only admin can create notice posts" }
        require(!pinned || isAdmin) { "only admin can pin posts" }

        val board = Board.create(
            memberId = memberId,
            boardType = boardType,
            title = title,
            content = content,
            anonymous = anonymous,
            guestPasswordHash = guestPasswordHash,
            notice = notice,
            pinned = pinned
        )
        val saved = command.save(board)
        log.info("[BOARD_CREATED] id={}, memberId={}", saved.id, memberId ?: -1L)
        return saved
    }

    @Transactional
    fun update(
        boardId: Long,
        requesterId: Long?,
        isAdmin: Boolean,
        title: String,
        content: String,
        anonymous: Boolean,
        guestPassword: String?,
        notice: Boolean,
        pinned: Boolean
    ) {
        val board = command.findById(boardId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다: $boardId")

        require(!board.deleted) { "삭제된 게시글은 수정할 수 없습니다" }
        require(!notice || isAdmin) { "only admin can set notice posts" }
        require(!pinned || isAdmin) { "only admin can pin posts" }

        validatePermission(
            board = board,
            requesterId = requesterId,
            isAdmin = isAdmin,
            guestPassword = guestPassword,
            action = "수정"
        )

        board.update(
            title = title,
            content = content,
            anonymous = anonymous,
            notice = if (isAdmin) notice else board.notice,
            pinned = if (isAdmin) pinned else board.pinned
        )
        command.save(board)
        log.info("[BOARD_UPDATED] id={}, requesterId={}", boardId, requesterId ?: -1L)
    }

    @Transactional
    fun pin(
        boardId: Long,
        pinned: Boolean
    ) {
        val board = command.findById(boardId)
            ?: throw IllegalArgumentException("board not found: $boardId")
        require(!board.deleted) { "cannot pin deleted board" }
        board.applyPinned(pinned)
        command.save(board)
        log.info("[BOARD_PIN_UPDATED] id={}, pinned={}", boardId, pinned)
    }

    @Transactional
    fun delete(
        boardId: Long,
        requesterId: Long?,
        isAdmin: Boolean,
        guestPassword: String?
    ) {
        val board = command.findById(boardId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다: $boardId")

        validatePermission(
            board = board,
            requesterId = requesterId,
            isAdmin = isAdmin,
            guestPassword = guestPassword,
            action = "삭제"
        )

        board.softDelete()
        command.save(board)
        log.info("[BOARD_DELETED] id={}, requesterId={}", boardId, requesterId ?: -1L)
    }

    private fun validatePermission(
        board: Board,
        requesterId: Long?,
        isAdmin: Boolean,
        guestPassword: String?,
        action: String
    ) {
        if (isAdmin) return

        if (!board.isGuestPost()) {
            require(requesterId != null && board.isWrittenBy(requesterId)) { "$action 권한이 없습니다" }
            return
        }

        val hash = board.guestPasswordHash
        require(!hash.isNullOrBlank()) { "게스트 게시글 비밀번호 정보가 없어 $action 할 수 없습니다" }
        val raw = guestPassword?.trim()
        require(!raw.isNullOrBlank()) { "비로그인 사용자는 비밀번호를 입력해야 합니다" }
        require(passwordEncoder.matches(raw, hash)) { "비밀번호가 일치하지 않습니다" }
    }
}
