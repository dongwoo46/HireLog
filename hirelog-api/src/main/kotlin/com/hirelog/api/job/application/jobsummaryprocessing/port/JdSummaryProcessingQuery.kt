package com.hirelog.api.job.application.jdsummaryprocessing.port

import com.hirelog.api.job.domain.JdSummaryProcessing
import com.hirelog.api.job.domain.JdSummaryProcessingStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * JdSummaryProcessing Query Port
 *
 * 역할:
 * - Processing 상태 조회 책임 추상화
 * - 조회 로직을 Application에서 분리
 */
interface JdSummaryProcessingQuery {

    /**
     * ID 기준 단건 조회
     */
    fun findById(id: UUID): JdSummaryProcessing?

    /**
     * Stuck 상태 Processing 조회
     *
     * 조건:
     * - status = SUMMARIZING
     * - llmResultJson IS NOT NULL
     * - updatedAt < olderThan
     *
     * 용도:
     * - Post-LLM 트랜잭션 실패 후 복구 대상 조회
     */
    fun findStuckWithLlmResult(
        status: JdSummaryProcessingStatus,
        olderThan: LocalDateTime,
        limit: Int
    ): List<JdSummaryProcessing>

}
