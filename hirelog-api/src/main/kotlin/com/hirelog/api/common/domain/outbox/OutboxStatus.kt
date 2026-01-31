package com.hirelog.api.common.domain.outbox

/**
 * OutboxStatus
 *
 * 의미:
 * - Outbox 이벤트의 발행 상태
 *
 * 상태 전이:
 * - PENDING  → PUBLISHED
 * - PENDING  → FAILED (재시도 대상)
 */
enum class OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
