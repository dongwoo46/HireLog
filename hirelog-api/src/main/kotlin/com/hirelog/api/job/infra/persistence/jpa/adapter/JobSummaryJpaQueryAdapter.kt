package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchCondition
import com.hirelog.api.job.application.summary.view.JobSummaryView
import com.hirelog.api.job.infra.persistence.jpa.mapper.summary.toSummaryView
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSummaryJpaQueryDslRepository
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSummaryJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * JobSummary JPA Query Adapter
 *
 * 책임:
 * - JobSummaryQuery Port 구현
 * - QueryDSL 기반 조회 수행
 *
 * 설계 원칙:
 * - Entity는 절대 외부로 노출하지 않는다
 * - 조회 조건은 유스케이스 모델(JobSummarySearchCondition)로 받는다
 * - 조회 결과는 Read Model(View)로만 반환한다
 */
@Component
class JobSummaryJpaQueryAdapter(
    private val queryDslRepository: JobSummaryJpaQueryDslRepository,
    private val jpaRepository: JobSummaryJpaRepository
) : JobSummaryQuery {

    /**
     * JobSummary 검색
     *
     * 흐름:
     * 1. QueryDSL Repository를 통해 Entity 조회
     * 2. Entity → View 변환 (Mapper 사용)
     * 3. Page<View> 형태로 반환
     */
    override fun search(
        condition: JobSummarySearchCondition,
        pageable: Pageable
    ): Page<JobSummaryView> {
        // 1️⃣ QueryDSL 기반 Entity 조회
        val projectionPage = queryDslRepository.search(
            brandId = condition.brandId,
            positionId = condition.positionId,
            keyword = condition.keyword,
            pageable = pageable
        )

        // 2️⃣ Entity → Read Model(View) 변환 후 반환
        return PageImpl(
            projectionPage.content.map { it.toSummaryView() }, // ✅ Projection → View
            pageable,
            projectionPage.totalElements
        )
    }

    override fun existsBySourceUrl(sourceUrl: String): Boolean {
        return jpaRepository.existsBySourceUrl(sourceUrl)
    }

    override fun existsByJobSnapshotId(jobSnapshotId: Long): Boolean {
        return jpaRepository.existsByJobSnapshotId(jobSnapshotId)
    }

    override fun findIdByJobSnapshotId(jobSnapshotId: Long): Long? {
        return jpaRepository.findByJobSnapshotId(jobSnapshotId)?.id
    }

    override fun findBySourceUrl(sourceUrl: String): JobSummaryView? {
        val entity = jpaRepository.findBySourceUrl(sourceUrl) ?: return null

        return JobSummaryView(
            summaryId = entity.id,
            snapshotId = entity.jobSnapshotId,
            brandId = entity.brandId,
            brandName = entity.brandName,
            positionId = entity.positionId,
            positionName = entity.positionName,
            careerType = entity.careerType,
            careerYears = entity.careerYears?.filter { it.isDigit() }?.toIntOrNull(),
            summary = entity.summaryText,
            responsibilities = entity.responsibilities,
            requiredQualifications = entity.requiredQualifications,
            preferredQualifications = entity.preferredQualifications,
            techStack = entity.techStack
        )
    }
}
