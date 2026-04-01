package com.hirelog.api.comment.application.port

import com.hirelog.api.comment.application.view.CommentLikeStat

interface CommentLikeQuery {
    fun getStat(commentId: Long, memberId: Long): CommentLikeStat
}
