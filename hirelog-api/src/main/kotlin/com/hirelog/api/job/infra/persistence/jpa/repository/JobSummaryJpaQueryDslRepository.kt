package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.JobSummary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface JobSummaryJpaQueryDslRepository {
    fun search(
        brandId: Long?,
        positionId: Long?,
        keyword: String?,
        pageable: Pageable
    ): Page<JobSummary>
}
