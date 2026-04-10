package com.hirelog.api.notification.application.view

import com.hirelog.api.common.application.port.PagedResult

data class NotificationPageView(
    val notifications: PagedResult<NotificationView>,
    val unreadCount: Long
)
