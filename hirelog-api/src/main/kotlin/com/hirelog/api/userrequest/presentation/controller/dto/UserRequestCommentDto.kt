package com.hirelog.api.userrequest.presentation.controller.dto

import com.hirelog.api.userrequest.domain.UserRequestComment
import com.hirelog.api.userrequest.domain.UserRequestCommentWriterType
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

/**
 * UserRequestComment 생성 요청 DTO
 */
data class UserRequestCommentCreateReq(
    @field:NotBlank
    val content: String
)



/**
 * UserRequestComment 응답 DTO
 */
data class UserRequestCommentRes(
    val id: Long,
    val userRequestId: Long,
    val writerType: UserRequestCommentWriterType,
    val writerId: Long,
    val content: String,
    val createdAt: LocalDateTime
) {
    companion object {

        fun from(
            entity: UserRequestComment,
            userRequestId: Long
        ): UserRequestCommentRes {
            return UserRequestCommentRes(
                id = entity.id,
                userRequestId = userRequestId,
                writerType = entity.writerType,
                writerId = entity.writerId,
                content = entity.content,
                createdAt = entity.createdAt
            )
        }
    }
}
