package com.hirelog.api.common.infra.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

/**
 * ProcessedEventJpaId
 *
 * 의미:
 * - processed_event 테이블의 복합 PK
 * - (event_id, consumer_group)
 *
 * 주의:
 * - JPA 전용 식별자
 * - 도메인 VO(ProcessedEventId)와 분리
 */
@Embeddable
class ProcessedEventJpaId(

    @Column(name = "event_id", nullable = false, length = 100)
    val eventId: String,

    @Column(name = "consumer_group", nullable = false, length = 100)
    val consumerGroup: String

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedEventJpaId) return false
        return eventId == other.eventId &&
                consumerGroup == other.consumerGroup
    }

    override fun hashCode(): Int {
        var result = eventId.hashCode()
        result = 31 * result + consumerGroup.hashCode()
        return result
    }
}
