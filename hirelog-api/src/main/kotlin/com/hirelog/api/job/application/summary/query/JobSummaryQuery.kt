package com.hirelog.api.job.application.summary.query

import org.springframework.data.domain.Page

/**
 * JobSummary 조회 포트
 *
 * 책임:
 * - JobSummary 조회 유스케이스 정의
 * - 저장소 구현(JPA / OpenSearch)을 추상화
 *
 * 설계 원칙:
 * - 조회 "의도"만 표현한다
 * - 조회 기술/스토리지는 infrastructure 책임이다
 */
interface JobSummaryQuery {

    /**
     * JobSummary 검색
     *
     * 사용 목적:
     * - 목록 조회
     * - 검색 결과 페이지 제공
     */
    fun search(
        brandId: Long?,
        positionId: Long?,
        keyword: String?,
        page: Int,
        size: Int
    ): Page<JobSummaryView>
}
