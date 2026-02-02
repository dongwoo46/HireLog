package com.hirelog.api.common.domain.outbox

/**
 * Outbox Aggregate Type
 *
 * 용도:
 * - Debezium CDC routing key
 * - Kafka topic 라우팅 (hirelog.outbox.{aggregateType})
 *
 * 규칙:
 * - 도메인 엔티티명과 일치
 * - PascalCase 유지 (Debezium topic naming 호환)
 */
enum class AggregateType(val value: String) {
    JOB_SUMMARY("JobSummary"),
    // 향후 추가될 Aggregate들
    // MEMBER("Member"),
    // BRAND("Brand"),
    ;

    override fun toString(): String = value
}
