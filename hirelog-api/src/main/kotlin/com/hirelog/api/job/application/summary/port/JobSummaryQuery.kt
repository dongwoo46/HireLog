package com.hirelog.api.job.application.summary.port

import com.hirelog.api.job.application.summary.query.JobSummarySearchCondition
import com.hirelog.api.job.application.summary.view.JobSummaryView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * JobSummary 조회 포트
 *
 * 책임:
 * - JobSummary 조회 유스케이스 정의
 * - 저장소 구현(JPA / OpenSearch)을 추상화
 */
interface JobSummaryQuery {

    /**
     * JobSummary 검색
     *
     * @param condition 조회 조건 (유스케이스 모델)
     * @param pageable 페이징 정보
     */
    fun search(
        condition: JobSummarySearchCondition,
        pageable: Pageable
    ): Page<JobSummaryView>
}
