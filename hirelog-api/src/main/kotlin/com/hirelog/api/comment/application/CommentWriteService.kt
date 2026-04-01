package com.hirelog.api.comment.application

import com.hirelog.api.comment.application.port.CommentCommand
import com.hirelog.api.comment.domain.Comment
import com.hirelog.api.common.logging.log
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentWriteService(
    private val command: CommentCommand
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    @Transactional
    fun write(
        boardId: Long,
        memberId: Long?,
        content: String,
        anonymous: Boolean,
        guestPassword: String?
    ): Comment {
        val guestPasswordHash = if (memberId == null) {
            val raw = guestPassword?.trim()
            require(!raw.isNullOrBlank()) { "비로그인 작성은 비밀번호를 입력해야 합니다." }
            passwordEncoder.encode(raw)
        } else {
            null
        }

        val comment = Comment.create(
            boardId = boardId,
            memberId = memberId,
            content = content,
            anonymous = anonymous,
            guestPasswordHash = guestPasswordHash
        )
        val saved = command.save(comment)
        log.info("[COMMENT_CREATED] id={}, boardId={}, memberId={}", saved.id, boardId, memberId ?: -1L)
        return saved
    }

    @Transactional
    fun update(
        commentId: Long,
        requesterId: Long?,
        isAdmin: Boolean,
        content: String,
        anonymous: Boolean,
        guestPassword: String?
    ) {
        val comment = command.findById(commentId)
            ?: throw IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId")

        require(!comment.deleted) { "삭제된 댓글은 수정할 수 없습니다" }
        validatePermission(
            comment = comment,
            requesterId = requesterId,
            isAdmin = isAdmin,
            guestPassword = guestPassword,
            action = "수정"
        )

        comment.update(content = content, anonymous = anonymous)
        command.save(comment)
        log.info("[COMMENT_UPDATED] id={}, requesterId={}", commentId, requesterId ?: -1L)
    }

    @Transactional
    fun delete(
        commentId: Long,
        requesterId: Long?,
        isAdmin: Boolean,
        guestPassword: String?
    ) {
        val comment = command.findById(commentId)
            ?: throw IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId")

        validatePermission(
            comment = comment,
            requesterId = requesterId,
            isAdmin = isAdmin,
            guestPassword = guestPassword,
            action = "삭제"
        )

        comment.softDelete()
        command.save(comment)
        log.info("[COMMENT_DELETED] id={}, requesterId={}", commentId, requesterId ?: -1L)
    }

    private fun validatePermission(
        comment: Comment,
        requesterId: Long?,
        isAdmin: Boolean,
        guestPassword: String?,
        action: String
    ) {
        if (isAdmin) return

        if (!comment.isGuestComment()) {
            require(requesterId != null && comment.isWrittenBy(requesterId)) { "$action 권한이 없습니다" }
            return
        }

        val hash = comment.guestPasswordHash
        require(!hash.isNullOrBlank()) { "게스트 댓글 비밀번호 정보가 없어 $action 할 수 없습니다" }
        val raw = guestPassword?.trim()
        require(!raw.isNullOrBlank()) { "비로그인 사용자는 비밀번호를 입력해야 합니다" }
        require(passwordEncoder.matches(raw, hash)) { "비밀번호가 일치하지 않습니다" }
    }
}

