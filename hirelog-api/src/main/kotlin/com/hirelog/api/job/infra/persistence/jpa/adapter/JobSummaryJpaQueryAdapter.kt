package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchCondition
import com.hirelog.api.job.application.summary.view.JobSummaryDetailView
import com.hirelog.api.job.application.summary.view.JobSummaryView
import com.hirelog.api.job.infra.persistence.jpa.mapper.summary.toSummaryView
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSummaryJpaQueryDslRepository
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSummaryJpaRepository
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberJobSummaryJpaRepository
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
    private val jpaRepository: JobSummaryJpaRepository,
    private val memberJobSummaryJpaRepository: MemberJobSummaryJpaRepository
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

    /**
     * JobSummary 상세 조회 (사용자 저장 상태 포함)
     *
     * 흐름:
     * 1. QueryDSL Projection으로 JobSummary 조회
     * 2. 현재 사용자의 MemberJobSummary 조회
     * 3. copy()로 합산
     *
     * 설계:
     * - Review는 별도 API(/api/job-summary/review/{jobSummaryId})로 분리
     */
    override fun findDetailById(jobSummaryId: Long, memberId: Long?): JobSummaryDetailView? {
        val detail = queryDslRepository.findDetailById(jobSummaryId) ?: return null

        if (memberId == null) {
            return detail
        }

        val memberJobSummary = memberJobSummaryJpaRepository
            .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)

        return detail.copy(
            memberJobSummaryId = memberJobSummary?.id,
            memberSaveType = memberJobSummary?.saveType?.name
        )
    }

    /**
     * URL 중복 체크 (활성화된 것만)
     *
     * 정책:
     * - 비활성화된 URL은 새로 생성 가능
     */
    override fun existsBySourceUrl(sourceUrl: String): Boolean {
        return jpaRepository.existsBySourceUrlAndIsActiveTrue(sourceUrl)
    }

    override fun existsByJobSnapshotId(jobSnapshotId: Long): Boolean {
        return jpaRepository.existsByJobSnapshotIdAndIsActiveTrue(jobSnapshotId)
    }

    override fun findIdByJobSnapshotId(jobSnapshotId: Long): Long? {
        return jpaRepository.findByJobSnapshotIdAndIsActiveTrue(jobSnapshotId)?.id
    }

    /**
     * URL로 조회 (활성화된 것만)
     */
    override fun findBySourceUrl(sourceUrl: String): JobSummaryView? {
        val entity = jpaRepository.findBySourceUrlAndIsActiveTrue(sourceUrl) ?: return null

        return JobSummaryView(
            summaryId = entity.id,
            snapshotId = entity.jobSnapshotId,
            brandId = entity.brandId,
            brandName = entity.brandName,
            positionId = entity.positionId,
            positionName = entity.positionName,
            brandPositionId = entity.brandPositionId,
            positionCategoryId = entity.positionCategoryId,
            positionCategoryName = entity.positionCategoryName,
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
