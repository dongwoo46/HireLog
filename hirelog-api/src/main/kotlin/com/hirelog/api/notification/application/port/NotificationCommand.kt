package com.hirelog.api.notification.application.port

import com.hirelog.api.notification.domain.model.Notification

interface NotificationCommand {
    fun save(notification: Notification): Notification
    fun findByIdsAndMemberId(ids: List<Long>, memberId: Long): List<Notification>
    fun saveAll(notifications: List<Notification>)
}
