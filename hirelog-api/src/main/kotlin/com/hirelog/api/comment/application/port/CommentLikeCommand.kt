package com.hirelog.api.comment.application.port

import com.hirelog.api.relation.domain.model.CommentLike

interface CommentLikeCommand {
    fun save(commentLike: CommentLike): CommentLike
    fun findByMemberIdAndCommentId(memberId: Long, commentId: Long): CommentLike?
    fun delete(commentLike: CommentLike)
}
