package com.hirelog.api.job.application.snapshot.command

import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.domain.JobSourceType

/**
 * JobSnapshot 생성 Command
 *
 * 책임:
 * - Snapshot 생성만 담당
 * - 중복 정책은 ApplicationService에서 결정
 */
interface JobSnapshotCommand {

    fun create(
        brandId: Long,
        companyId: Long?,
        positionId: Long,
        sourceType: JobSourceType,
        sourceUrl: String?,
        rawText: String,
        contentHash: String
    ): JobSnapshot
}
