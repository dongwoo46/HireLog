package com.hirelog.api.job.infrastructure.review.jpa

import com.hirelog.api.job.domain.JobSummaryReview
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable


/**
 * JobSummaryReview JPA Repository
 *
 * 책임:
 * - 순수 JPA 접근
 * - 비즈니스 로직 없음
 */
interface JobSummaryReviewJpaRepository :
    JpaRepository<JobSummaryReview, Long> {

    fun findByJobSummaryIdAndMemberId(
        jobSummaryId: Long,
        memberId: Long
    ): JobSummaryReview?

    fun findAllByJobSummaryId(
        jobSummaryId: Long
    ): List<JobSummaryReview>

    fun findAllByOrderByIdDesc(
        pageable: Pageable
    ): Page<JobSummaryReview>
}
