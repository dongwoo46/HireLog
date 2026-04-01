package com.hirelog.api.board.presentation.controller.dto.response

import com.hirelog.api.board.application.view.BoardView
import com.hirelog.api.board.domain.BoardType
import java.time.LocalDateTime

data class BoardRes(
    val id: Long,
    val boardType: BoardType,
    val title: String,
    val content: String,
    val authorUsername: String?,   // anonymous=true이면 null
    val anonymous: Boolean,
    val likeCount: Long,
    val commentCount: Long,
    val deleted: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(view: BoardView) = BoardRes(
            id = view.id,
            boardType = view.boardType,
            title = view.title,
            content = view.content,
            authorUsername = if (view.anonymous) null else view.authorUsername,
            anonymous = view.anonymous,
            likeCount = view.likeCount,
            commentCount = view.commentCount,
            deleted = view.deleted,
            createdAt = view.createdAt
        )
    }
}