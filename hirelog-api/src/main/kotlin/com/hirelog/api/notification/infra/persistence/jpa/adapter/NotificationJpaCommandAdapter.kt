package com.hirelog.api.notification.infra.persistence.jpa.adapter

import com.hirelog.api.notification.application.port.NotificationCommand
import com.hirelog.api.notification.domain.model.Notification
import com.hirelog.api.notification.infra.persistence.jpa.repository.NotificationJpaRepository
import org.springframework.stereotype.Component

@Component
class NotificationJpaCommandAdapter(
    private val repository: NotificationJpaRepository
) : NotificationCommand {

    override fun save(notification: Notification): Notification {
        return repository.save(notification)
    }

    override fun findByIdsAndMemberId(ids: List<Long>, memberId: Long): List<Notification> {
        return repository.findByIdInAndMemberId(ids, memberId)
    }

    override fun saveAll(notifications: List<Notification>) {
        repository.saveAll(notifications)
    }
}
