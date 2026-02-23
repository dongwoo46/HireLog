package com.hirelog.api.job.infra.kafka.topic

/**
 * Kafka Topic Names
 *
 * 책임:
 * - Kafka 토픽 이름 중앙 관리
 * - 인프라 식별자만 포함 (비즈니스 의미 없음)
 */
object JdKafkaTopics {


    const val PREPROCESS_RESPONSE     = "jd.preprocess.response"
    const val PREPROCESS_RESPONSE_FAIL = "jd.preprocess.response.fail"

    // Debezium CDC Outbox Topics
    const val OUTBOX_JOB_SUMMARY      = "hirelog.outbox.JobSummary"
}
