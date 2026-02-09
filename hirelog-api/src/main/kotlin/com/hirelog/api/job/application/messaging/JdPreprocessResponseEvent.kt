package com.hirelog.api.job.application.messaging

import com.hirelog.api.job.domain.type.JobSourceType
import java.time.LocalDate

/**
 * JdPreprocessResponseEvent
 *
 * Kafka Event
 *
 * 의미:
 * - JD 전처리 파이프라인이 성공적으로 완료되었음을 나타내는 이벤트
 *
 * 주의:
 * - Transport(Kafka) 의존 ❌
 * - Domain Logic ❌
 * - Validation ❌
 */
data class JdPreprocessResponseEvent(

    // === Event Identity ===
    val eventId: String,          // Kafka 메시지 단위 식별자
    val requestId: String,        // 파이프라인 correlation id

    // === Event Metadata ===
    val eventType: String,        // JD_PREPROCESS_COMPLETED
    val version: String,          // v1
    val occurredAt: Long,         // 이벤트 발생 시각

    // === Context ===
    val brandName: String,
    val positionName: String,

    // === Source ===
    val source: JobSourceType,
    val sourceUrl: String?,

    // === Canonical Result ===
    val canonicalMap: Map<String, List<String>>,

    // === Recruitment Info ===
    val recruitmentPeriodType: String?,
    val openedDate: LocalDate?,
    val closedDate: LocalDate?,

    // === Extracted Skills ===
    val skills: List<String>
)
