package com.hirelog.api.job.infrastructure.review.adapter

import com.hirelog.api.job.application.review.port.JobSummaryReviewQuery
import com.hirelog.api.job.application.review.port.PagedView
import com.hirelog.api.job.domain.model.JobSummaryReview
import com.hirelog.api.job.infrastructure.review.jpa.JobSummaryReviewJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * JobSummaryReview Query Adapter (JPA)
 *
 * 책임:
 * - Query Port를 JPA로 구현
 */
@Component
class JobSummaryReviewJpaQueryAdapter(
    private val repository: JobSummaryReviewJpaRepository
) : JobSummaryReviewQuery {

    override fun findByJobSummaryIdAndMemberId(
        jobSummaryId: Long,
        memberId: Long
    ): JobSummaryReview? {
        return repository.findByJobSummaryIdAndMemberId(jobSummaryId, memberId)
    }

    override fun findAllByJobSummaryId(
        jobSummaryId: Long
    ): List<JobSummaryReview> {
        return repository.findAllByJobSummaryId(jobSummaryId)
    }

    override fun findAllPaged(
        page: Int,
        size: Int
    ): PagedView<JobSummaryReview> {

        require(page >= 0) { "page는 0 이상이어야 합니다." }
        require(size in 1..100) { "size는 1~100 사이여야 합니다." }

        val pageable = PageRequest.of(page, size)
        val result = repository.findAllByOrderByIdDesc(pageable)

        return PagedView(
            items = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext()
        )
    }
}
