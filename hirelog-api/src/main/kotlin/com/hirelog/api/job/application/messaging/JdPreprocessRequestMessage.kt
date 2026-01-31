package com.hirelog.api.job.application.messaging

import com.hirelog.api.job.domain.JobSourceType

data class JdPreprocessRequestMessage(
    // === Event Identity ===
    val eventId: String,        // Kafka 메시지 1건 식별자 (UUID)
    val requestId: String,      // 파이프라인 트레이싱 ID

    // === Event Metadata ===
    val occurredAt: Long,
    val version: String,

    // === Business Payload ===
    val brandName: String,
    val positionName: String,
    val source: JobSourceType,
    val text: String
)
