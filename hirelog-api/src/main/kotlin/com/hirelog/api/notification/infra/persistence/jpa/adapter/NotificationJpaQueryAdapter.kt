package com.hirelog.api.notification.infra.persistence.jpa.adapter

import com.hirelog.api.notification.application.port.NotificationQuery
import com.hirelog.api.notification.application.view.NotificationView
import com.hirelog.api.notification.infra.persistence.jpa.repository.NotificationJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class NotificationJpaQueryAdapter(
    private val repository: NotificationJpaRepository
) : NotificationQuery {

    override fun findByMemberId(memberId: Long, isRead: Boolean?, page: Int, size: Int): List<NotificationView> {
        val pageable = PageRequest.of(page, size)
        val notifications = if (isRead != null) {
            repository.findByMemberIdAndIsReadOrderByCreatedAtDesc(memberId, isRead, pageable)
        } else {
            repository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
        }
        return notifications.map { NotificationView.from(it) }.content
    }

    override fun countByMemberId(memberId: Long, isRead: Boolean?): Long {
        return if (isRead != null) {
            repository.countByMemberIdAndIsRead(memberId, isRead)
        } else {
            repository.countByMemberId(memberId)
        }
    }

    override fun countUnreadByMemberId(memberId: Long): Long {
        return repository.countByMemberIdAndIsReadFalse(memberId)
    }
}
