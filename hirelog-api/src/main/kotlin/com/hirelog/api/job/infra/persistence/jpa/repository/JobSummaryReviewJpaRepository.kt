package com.hirelog.api.job.infrastructure.review.jpa

import com.hirelog.api.job.domain.model.JobSummaryReview
import org.springframework.data.jpa.repository.JpaRepository


/**
 * JobSummaryReview JPA Repository
 *
 * 책임:
 * - 순수 JPA 접근
 * - 비즈니스 로직 없음
 */
interface JobSummaryReviewJpaRepository :
    JpaRepository<JobSummaryReview, Long> {

    fun findByJobSummaryIdAndMemberIdAndDeletedFalse(
        jobSummaryId: Long,
        memberId: Long
    ): JobSummaryReview?
}
