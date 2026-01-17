package com.hirelog.api.job.repository

import com.hirelog.api.job.domain.JobSummary
import org.springframework.data.jpa.repository.JpaRepository

interface JobSummaryRepository : JpaRepository<JobSummary, Long> {

    fun findByJobSnapshotId(jobSnapshotId: Long): JobSummary?

    fun existsByJobSnapshotId(jobSnapshotId: Long): Boolean
}
