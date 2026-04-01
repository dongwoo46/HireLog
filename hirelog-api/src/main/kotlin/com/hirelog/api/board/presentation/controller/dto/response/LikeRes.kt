package com.hirelog.api.board.presentation.controller.dto.response

data class LikeRes(
    val likeCount: Long,
    val liked: Boolean
)