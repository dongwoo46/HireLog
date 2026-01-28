package com.hirelog.api.job.application.review.port

import com.hirelog.api.job.domain.JobSummaryReview

/**
 * JobSummaryReview Query Port
 *
 * 책임:
 * - 리뷰 조회 유스케이스 정의
 * - infra(Pageable/Page)에 의존하지 않음
 */
interface JobSummaryReviewQuery {

    /**
     * 특정 JD + 특정 회원 리뷰 조회
     */
    fun findByJobSummaryIdAndMemberId(
        jobSummaryId: Long,
        memberId: Long
    ): JobSummaryReview?

    /**
     * 특정 JD의 모든 리뷰 조회
     */
    fun findAllByJobSummaryId(
        jobSummaryId: Long
    ): List<JobSummaryReview>

    /**
     * 전체 리뷰 페이징 조회
     *
     * 정책:
     * - 최신순 정렬
     * - 페이지 기반 조회
     */
    fun findAllPaged(
        page: Int,
        size: Int
    ): PagedView<JobSummaryReview>
}