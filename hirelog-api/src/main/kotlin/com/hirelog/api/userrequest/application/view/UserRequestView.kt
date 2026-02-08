package com.hirelog.api.userrequest.application.view

import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.domain.UserRequestType
import java.time.LocalDateTime

data class UserRequestView(
    val id: Long,
    val memberId: Long,
    val title: String,
    val requestType: UserRequestType,
    val status: UserRequestStatus,
    val createdAt: LocalDateTime
)
