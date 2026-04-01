package com.hirelog.api.comment.application.view

import java.time.LocalDateTime

data class CommentView(
    val id: Long,
    val boardId: Long,
    val memberId: Long?,
    val authorUsername: String?,
    val content: String,
    val anonymous: Boolean,
    val likeCount: Long,
    val deleted: Boolean,
    val createdAt: LocalDateTime
)
