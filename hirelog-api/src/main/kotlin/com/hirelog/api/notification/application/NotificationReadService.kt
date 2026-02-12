package com.hirelog.api.notification.application

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.notification.application.port.NotificationQuery
import com.hirelog.api.notification.application.view.NotificationView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationReadService(
    private val notificationQuery: NotificationQuery
) {

    fun getNotifications(memberId: Long, isRead: Boolean?, page: Int, size: Int): PagedResult<NotificationView> {
        val items = notificationQuery.findByMemberId(memberId, isRead, page, size)
        val totalElements = notificationQuery.countByMemberId(memberId, isRead)

        return PagedResult.of(
            items = items,
            page = page,
            size = size,
            totalElements = totalElements
        )
    }

    fun getUnreadCount(memberId: Long): Long {
        return notificationQuery.countUnreadByMemberId(memberId)
    }
}
