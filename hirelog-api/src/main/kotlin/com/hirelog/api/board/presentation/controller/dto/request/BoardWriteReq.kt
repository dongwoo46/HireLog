package com.hirelog.api.board.presentation.controller.dto.request

import com.hirelog.api.board.domain.BoardType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class BoardWriteReq(
    @field:NotNull val boardType: BoardType,
    @field:NotBlank @field:Size(max = 300) val title: String,
    @field:NotBlank val content: String,
    @field:NotNull val anonymous: Boolean,
    @field:Size(min = 4, max = 100) val guestPassword: String? = null,
    val notice: Boolean = false,
    val pinned: Boolean = false
)
