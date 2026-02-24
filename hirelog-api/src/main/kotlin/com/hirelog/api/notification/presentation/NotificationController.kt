package com.hirelog.api.notification.presentation

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.notification.application.NotificationReadService
import com.hirelog.api.notification.application.NotificationWriteService
import com.hirelog.api.notification.application.view.NotificationView
import com.hirelog.api.notification.presentation.dto.CreateNotificationReq
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notification")
class NotificationController(
    private val notificationReadService: NotificationReadService,
    private val notificationWriteService: NotificationWriteService
) {

    @PostMapping
    fun createNotification(
        @RequestBody request: CreateNotificationReq
    ): ResponseEntity<Void> {

        notificationWriteService.create(
            memberId = request.memberId,
            type = request.type,
            title = request.title,
            message = request.message,
            referenceType = request.referenceType,
            referenceId = request.referenceId,
            metadata = request.metadata
        )

        return ResponseEntity.noContent().build()

    }

    @GetMapping
    fun getNotifications(
        @CurrentUser member: AuthenticatedMember,
        @RequestParam(required = false) isRead: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<NotificationView>> {
        val result = notificationReadService.getNotifications(
            memberId = member.memberId,
            isRead = isRead,
            page = page,
            size = size
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Map<String, Long>> {
        val count = notificationReadService.getUnreadCount(member.memberId)
        return ResponseEntity.ok(mapOf("unreadCount" to count))
    }

    @PatchMapping("/read")
    fun markAsRead(
        @CurrentUser member: AuthenticatedMember,
        @RequestBody notificationIds: List<Long>
    ): ResponseEntity<Void> {
        notificationWriteService.markAsRead(notificationIds = notificationIds, memberId = member.memberId)
        return ResponseEntity.noContent().build()
    }
}
