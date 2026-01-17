package com.hirelog.api.job.repository

import com.hirelog.api.job.domain.JobSnapshot
import org.springframework.data.jpa.repository.JpaRepository

interface JobSnapshotRepository : JpaRepository<JobSnapshot, Long> {

    fun findByContentHash(contentHash: String): JobSnapshot?

    fun existsByContentHash(contentHash: String): Boolean

    fun findAllByCompanyIdAndPositionId(
        companyId: Long,
        positionId: Long
    ): List<JobSnapshot>
}
