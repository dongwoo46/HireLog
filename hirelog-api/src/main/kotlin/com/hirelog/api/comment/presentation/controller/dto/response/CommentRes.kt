package com.hirelog.api.comment.presentation.controller.dto.response

import com.hirelog.api.comment.application.view.CommentView
import java.time.LocalDateTime

data class CommentRes(
    val id: Long,
    val boardId: Long,
    val authorUsername: String?,   // anonymous=true이면 null
    val anonymous: Boolean,
    val content: String,
    val likeCount: Long,
    val deleted: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(view: CommentView) = CommentRes(
            id = view.id,
            boardId = view.boardId,
            authorUsername = if (view.anonymous) null else view.authorUsername,
            anonymous = view.anonymous,
            content = if (view.deleted) "(삭제된 댓글입니다)" else view.content,
            likeCount = view.likeCount,
            deleted = view.deleted,
            createdAt = view.createdAt
        )
    }
}