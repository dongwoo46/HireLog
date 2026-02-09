package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.summary.port.JobSummaryRequestCommand
import com.hirelog.api.job.domain.model.JobSummaryRequest
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSummaryRequestJpaRepository
import org.springframework.stereotype.Component

@Component
class JobSummaryRequestJpaCommandAdapter(
    private val repository: JobSummaryRequestJpaRepository
) : JobSummaryRequestCommand {

    override fun save(request: JobSummaryRequest): JobSummaryRequest {
        return repository.save(request)
    }

    override fun findByRequestIdAndStatus(
        requestId: String,
        status: JobSummaryRequestStatus
    ): JobSummaryRequest? {
        return repository.findByRequestIdAndStatus(requestId, status)
    }
}
