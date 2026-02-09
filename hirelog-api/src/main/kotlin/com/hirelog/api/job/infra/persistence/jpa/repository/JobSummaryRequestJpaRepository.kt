package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.model.JobSummaryRequest
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus
import org.springframework.data.jpa.repository.JpaRepository

interface JobSummaryRequestJpaRepository : JpaRepository<JobSummaryRequest, Long> {

    fun findAllByRequestIdAndStatus(
        requestId: String,
        status: JobSummaryRequestStatus
    ): List<JobSummaryRequest>
}
