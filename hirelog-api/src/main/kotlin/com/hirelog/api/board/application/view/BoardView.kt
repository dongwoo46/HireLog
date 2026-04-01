package com.hirelog.api.board.application.view

import com.hirelog.api.board.domain.BoardType
import java.time.LocalDateTime

data class BoardView(
    val id: Long,
    val memberId: Long,
    val authorUsername: String,
    val boardType: BoardType,
    val title: String,
    val content: String,
    val anonymous: Boolean,
    val likeCount: Long,
    val commentCount: Long,
    val deleted: Boolean,
    val createdAt: LocalDateTime
)