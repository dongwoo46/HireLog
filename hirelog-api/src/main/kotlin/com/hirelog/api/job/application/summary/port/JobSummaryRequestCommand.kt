package com.hirelog.api.job.application.summary.port

import com.hirelog.api.job.domain.model.JobSummaryRequest
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus

/**
 * JobSummaryRequest Command Port
 *
 * 책임:
 * - JobSummaryRequest 영속화
 * - requestId 기반 조회 (상태 변경용)
 */
interface JobSummaryRequestCommand {

    fun save(request: JobSummaryRequest): JobSummaryRequest

    fun findAllByRequestIdAndStatus(
        requestId: String,
        status: JobSummaryRequestStatus
    ): List<JobSummaryRequest>
}
