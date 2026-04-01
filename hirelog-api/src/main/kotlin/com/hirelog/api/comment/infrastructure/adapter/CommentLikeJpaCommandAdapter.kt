package com.hirelog.api.comment.infrastructure.adapter

import com.hirelog.api.comment.application.port.CommentLikeCommand
import com.hirelog.api.comment.infrastructure.CommentLikeJpaRepository
import com.hirelog.api.relation.domain.model.CommentLike
import org.springframework.stereotype.Component

@Component
class CommentLikeJpaCommandAdapter(
    private val repository: CommentLikeJpaRepository
) : CommentLikeCommand {

    override fun save(commentLike: CommentLike): CommentLike = repository.save(commentLike)

    override fun findByMemberIdAndCommentId(memberId: Long, commentId: Long): CommentLike? =
        repository.findByMemberIdAndCommentId(memberId, commentId)

    override fun delete(commentLike: CommentLike) = repository.delete(commentLike)
}