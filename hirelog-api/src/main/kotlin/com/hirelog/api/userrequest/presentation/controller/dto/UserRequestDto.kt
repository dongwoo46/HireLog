package com.hirelog.api.userrequest.presentation.controller.dto

import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.domain.UserRequestType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * UserRequest 생성 요청 DTO
 */
data class UserRequestCreateReq(
    @field:NotNull
    val requestType: UserRequestType,

    @field:NotBlank
    val content: String
)

/**
 * UserRequest 상태 변경 요청 DTO
 */
data class UserRequestStatusUpdateReq(
    @field:NotNull
    val status: UserRequestStatus
)

/**
 * UserRequest 응답 DTO
 */
data class UserRequestRes(
    val id: Long,
    val memberId: Long,
    val requestType: UserRequestType,
    val content: String,
    val status: UserRequestStatus,
    val resolvedAt: LocalDateTime?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: UserRequest): UserRequestRes {
            return UserRequestRes(
                id = entity.id,
                memberId = entity.memberId,
                requestType = entity.requestType,
                content = entity.content,
                status = entity.status,
                resolvedAt = entity.resolvedAt,
                createdAt = entity.createdAt
            )
        }
    }
}
