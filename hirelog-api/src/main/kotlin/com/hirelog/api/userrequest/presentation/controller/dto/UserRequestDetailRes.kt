package com.hirelog.api.userrequest.presentation.controller.dto

import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestComment
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.domain.UserRequestType
import java.time.LocalDateTime

/**
 * UserRequest 상세 조회 DTO (댓글 포함)
 *
 * 조회 전용 Read Model
 */
data class UserRequestDetailRes(
    val id: Long,
    val memberId: Long,
    val title: String,
    val requestType: UserRequestType,
    val content: String,
    val status: UserRequestStatus,
    val resolvedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val comments: List<UserRequestCommentRes>
) {
    companion object {

        fun from(entity: UserRequest): UserRequestDetailRes {
            return UserRequestDetailRes(
                id = entity.id,
                memberId = entity.memberId,
                title = entity.title,
                requestType = entity.requestType,
                content = entity.content,
                status = entity.status(),
                resolvedAt = entity.resolvedAt(),
                createdAt = entity.createdAt,
                comments = entity.getComments().map { comment ->
                    UserRequestCommentRes.from(
                        entity = comment,
                        userRequestId = entity.id
                    )
                }
            )
        }
    }
}
