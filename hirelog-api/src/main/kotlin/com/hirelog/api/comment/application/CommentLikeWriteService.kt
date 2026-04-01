package com.hirelog.api.comment.application

import com.hirelog.api.comment.application.port.CommentLikeCommand
import com.hirelog.api.common.logging.log
import com.hirelog.api.relation.domain.model.CommentLike
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentLikeWriteService(
    private val command: CommentLikeCommand
) {

    @Transactional
    fun like(commentId: Long, memberId: Long) {
        require(command.findByMemberIdAndCommentId(memberId, commentId) == null) {
            "이미 좋아요한 댓글입니다"
        }
        command.save(CommentLike.create(memberId = memberId, commentId = commentId))
        log.info("[COMMENT_LIKED] commentId={}, memberId={}", commentId, memberId)
    }

    @Transactional
    fun unlike(commentId: Long, memberId: Long) {
        val like = command.findByMemberIdAndCommentId(memberId, commentId)
            ?: throw IllegalArgumentException("좋아요한 댓글이 아닙니다")
        command.delete(like)
        log.info("[COMMENT_UNLIKED] commentId={}, memberId={}", commentId, memberId)
    }
}