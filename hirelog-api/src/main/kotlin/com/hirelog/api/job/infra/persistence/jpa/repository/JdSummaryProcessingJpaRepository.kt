package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.JdSummaryProcessing
import com.hirelog.api.job.domain.JdSummaryProcessingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

/**
 * JdSummaryProcessing JPA Repository
 *
 * 역할:
 * - Spring Data JPA 기본 CRUD 제공
 * - 비즈니스 로직 포함 ❌
 */
interface JdSummaryProcessingJpaRepository :
    JpaRepository<JdSummaryProcessing, UUID> {

    /**
     * Stuck 상태 Processing 조회
     *
     * 조건:
     * - 지정된 status
     * - llmResultJson IS NOT NULL
     * - updatedAt < olderThan
     */
    @Query("""
        SELECT p FROM JdSummaryProcessing p
        WHERE p.status = :status
          AND p.llmResultJson IS NOT NULL
          AND p.updatedAt < :olderThan
        ORDER BY p.updatedAt ASC
        LIMIT :limit
    """)
    fun findStuckWithLlmResult(
        @Param("status") status: JdSummaryProcessingStatus,
        @Param("olderThan") olderThan: LocalDateTime,
        @Param("limit") limit: Int
    ): List<JdSummaryProcessing>
}
