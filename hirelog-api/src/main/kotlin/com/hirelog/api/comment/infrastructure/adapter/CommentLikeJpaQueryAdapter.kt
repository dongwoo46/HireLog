package com.hirelog.api.comment.infrastructure.adapter

import com.hirelog.api.comment.application.port.CommentLikeQuery
import com.hirelog.api.comment.application.view.CommentLikeStat
import com.hirelog.api.comment.infrastructure.CommentLikeJpaRepository
import org.springframework.stereotype.Component

@Component
class CommentLikeJpaQueryAdapter(
    private val repository: CommentLikeJpaRepository
) : CommentLikeQuery {

    override fun getStat(commentId: Long, memberId: Long): CommentLikeStat {
        return CommentLikeStat(
            likeCount = repository.countByCommentId(commentId),
            liked = repository.existsByMemberIdAndCommentId(memberId, commentId)
        )
    }
}