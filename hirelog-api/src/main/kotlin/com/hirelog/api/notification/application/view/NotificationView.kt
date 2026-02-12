package com.hirelog.api.notification.application.view

import com.hirelog.api.notification.domain.model.Notification
import com.hirelog.api.notification.domain.type.NotificationReferenceType
import com.hirelog.api.notification.domain.type.NotificationType
import java.time.LocalDateTime

data class NotificationView(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val message: String?,
    val referenceType: NotificationReferenceType?,
    val referenceId: Long?,
    val metadata: Map<String, Any?>,
    val isRead: Boolean,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(notification: Notification): NotificationView {
            return NotificationView(
                id = notification.id,
                type = notification.type,
                title = notification.title,
                message = notification.message,
                referenceType = notification.referenceType,
                referenceId = notification.referenceId,
                metadata = notification.metadata,
                isRead = notification.isRead,
                readAt = notification.readAt,
                createdAt = notification.createdAt
            )
        }
    }
}
