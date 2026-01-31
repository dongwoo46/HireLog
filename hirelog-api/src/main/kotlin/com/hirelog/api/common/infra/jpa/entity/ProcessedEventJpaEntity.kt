package com.hirelog.api.common.infra.jpa.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * ProcessedEventJpaEntity
 *
 * 역할:
 * - processed_event 테이블과의 JPA 매핑
 *
 * 주의:
 * - 비즈니스 로직 없음
 * - 멱등성 판단 로직 없음
 * - 상태 저장만 담당
 */
@Entity
@Table(name = "processed_event")
class ProcessedEventJpaEntity(

    @EmbeddedId // 복합 pk (이벤트ID, 컨슈머그룹)
    val id: ProcessedEventJpaId,

    @Column(name = "processed_at", nullable = false)
    val processedAt: LocalDateTime

) {

    companion object {

        /**
         * 신규 처리 이벤트 생성
         */
        fun create(
            eventId: String,
            consumerGroup: String,
            processedAt: LocalDateTime = LocalDateTime.now()
        ): ProcessedEventJpaEntity {
            return ProcessedEventJpaEntity(
                id = ProcessedEventJpaId(
                    eventId = eventId,
                    consumerGroup = consumerGroup
                ),
                processedAt = processedAt
            )
        }
    }
}
