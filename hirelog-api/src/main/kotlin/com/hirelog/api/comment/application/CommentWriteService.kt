package com.hirelog.api.comment.application

import com.hirelog.api.comment.application.port.CommentCommand
import com.hirelog.api.comment.domain.Comment
import com.hirelog.api.common.logging.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentWriteService(
    private val command: CommentCommand
) {

    @Transactional
    fun write(
        boardId: Long,
        memberId: Long?,
        content: String,
        anonymous: Boolean
    ): Comment {
        val comment = Comment.create(
            boardId = boardId,
            memberId = memberId,
            content = content,
            anonymous = anonymous
        )
        val saved = command.save(comment)
        log.info("[COMMENT_CREATED] id={}, boardId={}, memberId={}", saved.id, boardId, memberId ?: -1L)
        return saved
    }

    @Transactional
    fun update(
        commentId: Long,
        requesterId: Long,
        isAdmin: Boolean,
        content: String,
        anonymous: Boolean
    ) {
        val comment = command.findById(commentId)
            ?: throw IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId")

        require(!comment.deleted) { "삭제된 댓글은 수정할 수 없습니다" }
        require(isAdmin || comment.isWrittenBy(requesterId)) { "수정 권한이 없습니다" }

        comment.update(content = content, anonymous = anonymous)
        command.save(comment)
        log.info("[COMMENT_UPDATED] id={}, requesterId={}", commentId, requesterId)
    }

    @Transactional
    fun delete(commentId: Long, requesterId: Long, isAdmin: Boolean) {
        val comment = command.findById(commentId)
            ?: throw IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId")

        require(isAdmin || comment.isWrittenBy(requesterId)) { "삭제 권한이 없습니다" }

        comment.softDelete()
        command.save(comment)
        log.info("[COMMENT_DELETED] id={}, requesterId={}", commentId, requesterId)
    }
}
