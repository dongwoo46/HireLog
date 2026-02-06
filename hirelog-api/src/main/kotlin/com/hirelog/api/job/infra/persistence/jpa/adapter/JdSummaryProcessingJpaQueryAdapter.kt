package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.domain.JdSummaryProcessing
import com.hirelog.api.job.domain.JdSummaryProcessingStatus
import com.hirelog.api.job.infra.persistence.jpa.repository.JdSummaryProcessingJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

/**
 * JdSummaryProcessing JPA Query Adapter
 *
 * 역할:
 * - Query Port의 JPA 구현체
 * - 조회 전용 책임 수행
 */
@Component
class JdSummaryProcessingJpaQueryAdapter(
    private val repository: JdSummaryProcessingJpaRepository
) : JdSummaryProcessingQuery {

    override fun findById(id: UUID): JdSummaryProcessing? =
        repository.findById(id).orElse(null)

    override fun findStuckWithLlmResult(
        status: JdSummaryProcessingStatus,
        olderThan: LocalDateTime,
        limit: Int
    ): List<JdSummaryProcessing> =
        repository.findStuckWithLlmResult(status, olderThan, limit)

}
