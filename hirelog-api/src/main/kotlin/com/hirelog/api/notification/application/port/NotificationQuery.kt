package com.hirelog.api.notification.application.port

import com.hirelog.api.notification.application.view.NotificationView

interface NotificationQuery {
    fun findByMemberId(memberId: Long, isRead: Boolean?, page: Int, size: Int): List<NotificationView>
    fun countByMemberId(memberId: Long, isRead: Boolean?): Long
    fun countUnreadByMemberId(memberId: Long): Long
}
