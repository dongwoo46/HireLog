package com.hirelog.api.job.application.review.port

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.summary.view.JobSummaryReviewView
import com.hirelog.api.job.domain.type.HiringStage

/**
 * JobSummaryReview Query Port
 *
 * 책임:
 * - 리뷰 조회 유스케이스 정의 (View 반환)
 * - infra(Pageable/Page)에 의존하지 않음
 */
interface JobSummaryReviewQuery {

    /**
     * 특정 JobSummary의 리뷰 페이징 + 필터 조회
     *
     * 필터:
     * - hiringStage: 전형 단계
     * - minDifficultyRating / maxDifficultyRating: 난이도 범위
     * - minSatisfactionRating / maxSatisfactionRating: 만족도 범위
     *
     * 정책:
     * - soft delete된 리뷰 제외
     * - 최신순 정렬
     */
    fun findByJobSummaryId(
        jobSummaryId: Long,
        hiringStage: HiringStage?,
        minDifficultyRating: Int?,
        maxDifficultyRating: Int?,
        minSatisfactionRating: Int?,
        maxSatisfactionRating: Int?,
        page: Int,
        size: Int
    ): PagedResult<JobSummaryReviewView>
}
