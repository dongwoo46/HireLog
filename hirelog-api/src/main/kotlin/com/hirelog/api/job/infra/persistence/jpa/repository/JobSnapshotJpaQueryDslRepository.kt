package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.JobSnapshot
import java.time.LocalDate

interface JobSnapshotJpaQueryDslRepository {
    fun findAllOverlappingDateRange(
        openedDate: LocalDate?,
        closedDate: LocalDate?
    ): List<JobSnapshot>
}
