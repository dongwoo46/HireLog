package com.hirelog.api.job.application.review.port

import com.hirelog.api.job.domain.model.JobSummaryReview


/**
 * JobSummaryReview Command Port
 *
 * 책임:
 * - 리뷰 생성/삭제 명령 정의
 * - 영속성 기술로부터 완전히 분리
 */
interface JobSummaryReviewCommand {

    /**
     * 리뷰 저장 (생성 전용)
     */
    fun save(review: JobSummaryReview): JobSummaryReview

    /**
     * 특정 JD + 특정 회원 리뷰 엔티티 조회 (중복 체크용)
     */
    fun findByJobSummaryIdAndMemberId(
        jobSummaryId: Long,
        memberId: Long
    ): JobSummaryReview?

    /**
     * 리뷰 ID로 엔티티 조회
     */
    fun findById(reviewId: Long): JobSummaryReview?
}
