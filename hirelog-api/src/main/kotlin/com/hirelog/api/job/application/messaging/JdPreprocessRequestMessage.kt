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

    // === Source별 데이터 (Python 측 필수 필드) ===
    val text: String? = null,           // TEXT 소스용
    val url: String? = null,            // URL 소스용
    val images: List<String>? = null    // IMAGE(OCR) 소스용
)
