package com.hirelog.api.job.infrastructure.review.adapter

import com.hirelog.api.job.application.review.port.JobSummaryReviewCommand
import com.hirelog.api.job.domain.JobSummaryReview
import com.hirelog.api.job.infrastructure.review.jpa.JobSummaryReviewJpaRepository
import org.springframework.stereotype.Component

/**
 * JobSummaryReview Command Adapter (JPA)
 *
 * 책임:
 * - Command Port를 JPA로 구현
 */
@Component
class JobSummaryReviewJpaCommandAdapter(
    private val repository: JobSummaryReviewJpaRepository
) : JobSummaryReviewCommand {

    override fun save(review: JobSummaryReview): JobSummaryReview {
        return repository.save(review)
    }
}
