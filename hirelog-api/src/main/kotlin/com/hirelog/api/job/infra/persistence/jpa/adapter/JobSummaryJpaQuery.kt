package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.summary.query.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummaryView
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSummaryJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * JobSummary 조회 JPA 구현체
 *
 * 역할:
 * - JobSummaryQuery Port 구현
 * - 검색/페이징 유스케이스 담당
 *
 * 설계 의도:
 * - 조회 기술(JPA)을 application에서 분리
 * - OpenSearch 전환 시 교체 지점 확보
 */
@Component
class JobSummaryJpaQuery(
    private val repository: JobSummaryJpaRepository
) : JobSummaryQuery {

    /**
     * JobSummary 검색
     *
     * 주의:
     * - 실제 서비스에서는 QueryDSL / JPQL로 대체 권장
     * - 여기서는 구조 예시를 위해 단순 구현
     */
    override fun search(
        brandId: Long?,
        positionId: Long?,
        keyword: String?,
        page: Int,
        size: Int
    ): Page<JobSummaryView> {

        val pageable = PageRequest.of(page, size)

        val entityPage = repository.findAll(pageable)

        val views = entityPage.content.map { entity ->
            JobSummaryView(
                summaryId = entity.id,
                snapshotId = entity.jobSnapshotId,
                brandId = entity.brandId,
                brandName = entity.brandName,
                positionId = entity.positionId,
                positionName = entity.positionName,
                careerType = entity.careerType,
                careerYears = entity.careerYears,
                summary = entity.summaryText,
                responsibilities = entity.responsibilities,
                requiredQualifications = entity.requiredQualifications,
                preferredQualifications = entity.preferredQualifications,
                techStack = entity.techStack
            )
        }

        return PageImpl(views, pageable, entityPage.totalElements)
    }
}
