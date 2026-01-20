package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchCondition
import com.hirelog.api.job.application.summary.view.JobSummaryView
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSummaryJpaQueryDslRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * JobSummary 조회 JPA Adapter
 *
 * 책임:
 * - JobSummaryQuery Port 구현
 * - QueryDSL 기반 검색 수행
 *
 * 설계 원칙:
 * - Entity는 외부로 노출하지 않는다
 * - 조회 조건은 유스케이스 모델(condition)로 받는다
 */
@Component
class JobSummaryJpaQuery(
    private val queryDslRepository: JobSummaryJpaQueryDslRepository
) : JobSummaryQuery {

    override fun search(
        condition: JobSummarySearchCondition,
        pageable: Pageable
    ): Page<JobSummaryView> {

        // 1️⃣ QueryDSL 조회
        val entityPage = queryDslRepository.search(
            brandId = condition.brandId,
            positionId = condition.positionId,
            keyword = condition.keyword,
            pageable = pageable
        )

        // 2️⃣ Entity → View 변환
        return PageImpl(
            entityPage.content.map { it.toView() }, // ✅ 핵심
            pageable,
            entityPage.totalElements
        )
    }
}
