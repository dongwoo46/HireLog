package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.JobSummary
import org.springframework.data.jpa.repository.JpaRepository

/**
 * JobSummary JPA Repository
 *
 * 책임:
 * - JobSummary Entity CRUD
 */
interface JobSummaryJpaRepository : JpaRepository<JobSummary, Long> {

    /**
     * sourceUrl 중복 체크 (활성화된 것만)
     */
    fun existsBySourceUrlAndIsActiveTrue(sourceUrl: String): Boolean

    /**
     * sourceUrl 중복 체크 (전체 - Admin용)
     */
    fun existsBySourceUrl(sourceUrl: String): Boolean

    fun existsByJobSnapshotId(jobSnapshotId: Long): Boolean

    fun findByJobSnapshotId(jobSnapshotId: Long): JobSummary?

    /**
     * sourceUrl로 조회 (활성화된 것만)
     */
    fun findBySourceUrlAndIsActiveTrue(sourceUrl: String): JobSummary?

    fun findBySourceUrl(sourceUrl: String): JobSummary?
}
