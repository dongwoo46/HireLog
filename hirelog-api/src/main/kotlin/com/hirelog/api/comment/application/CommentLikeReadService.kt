package com.hirelog.api.comment.application

import com.hirelog.api.comment.application.port.CommentLikeQuery
import com.hirelog.api.comment.application.view.CommentLikeStat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentLikeReadService(
    private val query: CommentLikeQuery
) {

    @Transactional(readOnly = true)
    fun getStat(commentId: Long, memberId: Long): CommentLikeStat {
        return query.getStat(commentId = commentId, memberId = memberId)
    }
}