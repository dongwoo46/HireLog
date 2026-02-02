package com.hirelog.api.job.infra.kafka.topic

/**
 * Kafka Topic Names
 *
 * 책임:
 * - Kafka 토픽 이름 중앙 관리
 * - 인프라 식별자만 포함 (비즈니스 의미 없음)
 */
object JdKafkaTopics {

    const val PREPROCESS_TEXT_REQUEST = "jd.preprocess.text.request"
    const val PREPROCESS_OCR_REQUEST  = "jd.preprocess.ocr.request"
    const val PREPROCESS_URL_REQUEST  = "jd.preprocess.url.request"

    const val PREPROCESS_RESPONSE     = "jd.preprocess.response"

    // Debezium CDC Outbox Topics
    const val OUTBOX_JOB_SUMMARY      = "hirelog.outbox.JobSummary"
}
