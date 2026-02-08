package com.hirelog.api.common.infra.persistence.jpa.repository

import com.hirelog.api.common.domain.kafka.FailedKafkaEvent
import com.hirelog.api.common.domain.kafka.FailedEventStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/**
 * FailedKafkaEvent JPA Repository
 */
interface FailedKafkaEventJpaRepository : JpaRepository<FailedKafkaEvent, Long> {

    fun findByStatus(status: FailedEventStatus, pageable: Pageable): Page<FailedKafkaEvent>

    fun findByTopicAndStatus(topic: String, status: FailedEventStatus, pageable: Pageable): Page<FailedKafkaEvent>

    fun countByStatus(status: FailedEventStatus): Long
}
