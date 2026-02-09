package com.hirelog.api.job.application.summary.event

/**
 * JobSummary 파이프라인 완료/실패 이벤트
 *
 * 용도:
 * - 핵심 트랜잭션(JobSummary + Outbox + Processing) 커밋 후
 *   부가 작업(Request 상태 전이, SSE 알림)을 트리거
 *
 * 발행 시점:
 * - Completed: createWithOutbox() 트랜잭션 내부에서 publishEvent()
 * - Failed: markFailed() 트랜잭션 내부에서 publishEvent()
 *
 * 소비 시점:
 * - @TransactionalEventListener(AFTER_COMMIT)
 */
sealed class JobSummaryRequestEvent {

    data class Completed(
        val processingId: String,
        val jobSummaryId: Long,
        val brandName: String,
        val positionName: String,
        val brandPositionName: String,
        val positionCategoryName: String
    ) : JobSummaryRequestEvent()

    data class Failed(
        val processingId: String,
        val errorCode: String,
        val retryable: Boolean
    ) : JobSummaryRequestEvent() {

        companion object {
            private val RETRYABLE_ERROR_CODES = setOf(
                "LLM_TIMEOUT",
                "LLM_CALL_FAILED"
            )

            fun of(processingId: String, errorCode: String): Failed =
                Failed(
                    processingId = processingId,
                    errorCode = errorCode,
                    retryable = errorCode in RETRYABLE_ERROR_CODES
                )
        }
    }
}
