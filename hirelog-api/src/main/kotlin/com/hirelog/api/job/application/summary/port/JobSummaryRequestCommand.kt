package com.hirelog.api.job.application.summary.port

import com.hirelog.api.job.domain.model.JobSummaryRequest
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus

/**
 * JobSummaryRequest Command Port
 *
 * 책임:
 * - JobSummaryRequest 영속화
 * - requestId 기반 단건 조회 (상태 변경용)
 *
 * 정책:
 * - requestId:memberId = 1:1 (요청마다 고유 UUID 생성)
 */
interface JobSummaryRequestCommand {

    fun save(request: JobSummaryRequest): JobSummaryRequest

    fun findByRequestIdAndStatus(
        requestId: String,
        status: JobSummaryRequestStatus
    ): JobSummaryRequest?
}
