package com.hirelog.api.notification.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.notification.domain.type.NotificationReferenceType
import com.hirelog.api.notification.domain.type.NotificationType
import com.hirelog.api.notification.infra.jpa.MapJsonConverter
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification",
    indexes = [
        Index(
            name = "idx_notification_member_read",
            columnList = "member_id, is_read"
        ),
        Index(
            name = "idx_notification_member_created",
            columnList = "member_id, created_at"
        )
    ]
)
class Notification protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 50)
    val type: NotificationType,

    @Column(name = "title", nullable = false, updatable = false, length = 200)
    val title: String,

    @Column(name = "message", updatable = false, columnDefinition = "TEXT")
    val message: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", updatable = false, length = 50)
    val referenceType: NotificationReferenceType? = null,

    @Column(name = "reference_id", updatable = false)
    val referenceId: Long? = null,

    @Convert(converter = MapJsonConverter::class)
    @Column(name = "metadata", updatable = false, columnDefinition = "TEXT")
    val metadata: Map<String, Any?> = emptyMap(),

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null

) : BaseEntity() {

    companion object {

        fun create(
            memberId: Long,
            type: NotificationType,
            title: String,
            message: String? = null,
            referenceType: NotificationReferenceType? = null,
            referenceId: Long? = null,
            metadata: Map<String, Any?> = emptyMap()
        ): Notification {
            require(memberId > 0) { "memberId must be positive" }
            require(title.isNotBlank()) { "title must not be blank" }

            return Notification(
                memberId = memberId,
                type = type,
                title = title,
                message = message,
                referenceType = referenceType,
                referenceId = referenceId,
                metadata = metadata
            )
        }
    }

    fun markAsRead() {
        if (isRead) return
        isRead = true
        readAt = LocalDateTime.now()
    }
}
