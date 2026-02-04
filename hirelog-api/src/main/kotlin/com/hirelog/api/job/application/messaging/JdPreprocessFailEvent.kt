package com.hirelog.api.job.application.messaging

/**
 * JdPreprocessFailEvent
 *
 * Python 파이프라인에서 전처리 실패 시 발행하는 Kafka Event
 *
 * Topic: jd.preprocess.response.fail
 */
data class JdPreprocessFailEvent(

    // === Event Identity ===
    val eventId: String,
    val requestId: String,

    // === Event Metadata ===
    val eventType: String,        // JD_PREPROCESS_FAILED
    val version: String,          // v1
    val occurredAt: Long,

    // === Error Info ===
    val source: String,           // OCR / TEXT / URL
    val errorCode: String,
    val errorMessage: String,
    val errorCategory: String,    // RECOVERABLE / PERMANENT / UNKNOWN
    val pipelineStage: String,

    // === Debug Info ===
    val workerHost: String,
    val processingDurationMs: Int,

    // === Kafka Metadata (원본 메시지 위치) ===
    val kafkaMetadata: KafkaMetadata?
)

data class KafkaMetadata(
    val originalTopic: String?,
    val originalPartition: Int?,
    val originalOffset: Long?
)
