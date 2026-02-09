package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.domain.model.JobSummary
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSummaryJpaRepository
import org.springframework.stereotype.Component

/**
 * JobSummary JPA Command Adapter
 *
 * 책임:
 * - JobSummary 영속화
 *
 * 설계 원칙:
 * - Domain 생성 ❌
 * - 정책 판단 ❌
 * - JPA 저장만 수행
 */
@Component
class JobSummaryJpaCommandAdapter(
    private val repository: JobSummaryJpaRepository
) : JobSummaryCommand {

    override fun save(summary: JobSummary): JobSummary {
        return repository.save(summary)
    }

    override fun update(summary: JobSummary) {
        repository.save(summary)
    }

    override fun findById(id: Long): JobSummary? {
        return repository.findById(id).orElse(null)
    }
}
