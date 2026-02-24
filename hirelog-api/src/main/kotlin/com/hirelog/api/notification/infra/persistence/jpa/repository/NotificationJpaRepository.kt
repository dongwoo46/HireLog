package com.hirelog.api.notification.infra.persistence.jpa.repository

import com.hirelog.api.notification.domain.model.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationJpaRepository : JpaRepository<Notification, Long> {

    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long, pageable: Pageable): Page<Notification>

    fun findByMemberIdAndIsReadOrderByCreatedAtDesc(memberId: Long, isRead: Boolean, pageable: Pageable): Page<Notification>

    fun countByMemberId(memberId: Long): Long

    fun countByMemberIdAndIsRead(memberId: Long, isRead: Boolean): Long

    fun countByMemberIdAndIsReadFalse(memberId: Long): Long

    fun findByIdInAndMemberId(ids: List<Long>, memberId: Long): List<Notification>
}
