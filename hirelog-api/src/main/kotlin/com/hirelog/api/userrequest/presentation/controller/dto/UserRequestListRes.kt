package com.hirelog.api.userrequest.presentation.controller.dto


import com.hirelog.api.userrequest.application.view.UserRequestView
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.domain.UserRequestType
import java.time.LocalDateTime

/**
 * UserRequest 목록/관리자 조회 응답 DTO
 */
data class UserRequestListRes(
    val id: Long,
    val title: String,
    val requestType: UserRequestType,
    val status: UserRequestStatus,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(view: UserRequestView): UserRequestListRes {
            return UserRequestListRes(
                id = view.id,
                title = view.title,
                requestType = view.requestType,
                status = view.status,
                createdAt = view.createdAt
            )
        }
    }
}
