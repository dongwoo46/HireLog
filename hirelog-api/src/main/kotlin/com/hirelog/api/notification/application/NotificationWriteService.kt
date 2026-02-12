package com.hirelog.api.notification.application

import com.hirelog.api.common.logging.log
import com.hirelog.api.notification.application.port.NotificationCommand
import com.hirelog.api.notification.domain.model.Notification
import com.hirelog.api.notification.domain.type.NotificationReferenceType
import com.hirelog.api.notification.domain.type.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationWriteService(
    private val notificationCommand: NotificationCommand
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun create(
        memberId: Long,
        type: NotificationType,
        title: String,
        message: String? = null,
        referenceType: NotificationReferenceType? = null,
        referenceId: Long? = null,
        metadata: Map<String, Any?> = emptyMap()
    ): Notification {
        val notification = Notification.create(
            memberId = memberId,
            type = type,
            title = title,
            message = message,
            referenceType = referenceType,
            referenceId = referenceId,
            metadata = metadata
        )

        val saved = notificationCommand.save(notification)

        log.info(
            "[NOTIFICATION_CREATED] type={}, memberId={}, refType={}, refId={}",
            type, memberId, referenceType, referenceId
        )

        return saved
    }

    @Transactional
    fun markAsRead(notificationIds: List<Long>, memberId: Long) {
        val notifications = notificationCommand.findByIdsAndMemberId(notificationIds, memberId)

        if (notifications.isEmpty()) return

        notifications.forEach { it.markAsRead() }

        log.info("[NOTIFICATION_READ] memberId={}, ids={}", memberId, notifications.map { it.id })
    }
}
