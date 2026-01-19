package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.JobSummary
import org.springframework.data.jpa.repository.JpaRepository

/**
 * JobSummary JPA Repository
 *
 * 책임:
 * - JobSummary Entity CRUD
 */
interface JobSummaryJpaRepository : JpaRepository<JobSummary, Long>
