package com.hirelog.api.job.application.review.port

import com.hirelog.api.job.domain.model.JobSummaryReview


/**
 * JobSummaryReview Command Port
 *
 * 책임:
 * - 리뷰 생성/수정 명령 정의
 * - 영속성 기술로부터 완전히 분리
 */
interface JobSummaryReviewCommand {

    /**
     * 리뷰 저장 (생성/수정)
     */
    fun save(review: JobSummaryReview): JobSummaryReview
}
