package com.hirelog.api.comment.presentation.controller.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CommentWriteReq(
    @field:NotBlank @field:Size(max = 1000) val content: String,
    @field:NotNull val anonymous: Boolean,
    @field:Size(min = 4, max = 100) val guestPassword: String? = null
)
