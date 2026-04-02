package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchCondition
import com.hirelog.api.job.application.summary.view.JobSummaryAdminView
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
 * 梨낆엫:
 * - JobSummaryQuery Port 援ы쁽
 * - QueryDSL 湲곕컲 議고쉶 ?섑뻾
 *
 * ?ㅺ퀎 ?먯튃:
 * - Entity???덈? ?몃?濡??몄텧?섏? ?딅뒗?? * - 議고쉶 議곌굔? ?좎뒪耳?댁뒪 紐⑤뜽(JobSummarySearchCondition)濡?諛쏅뒗?? * - 議고쉶 寃곌낵??Read Model(View)濡쒕쭔 諛섑솚?쒕떎
 */
@Component
class JobSummaryJpaQueryAdapter(
    private val queryDslRepository: JobSummaryJpaQueryDslRepository,
    private val jpaRepository: JobSummaryJpaRepository,
    private val memberJobSummaryJpaRepository: MemberJobSummaryJpaRepository
) : JobSummaryQuery {

    /**
     * JobSummary 寃??     *
     * ?먮쫫:
     * 1. QueryDSL Repository瑜??듯빐 Entity 議고쉶
     * 2. Entity ??View 蹂??(Mapper ?ъ슜)
     * 3. Page<View> ?뺥깭濡?諛섑솚
     */
    override fun search(
        condition: JobSummarySearchCondition,
        pageable: Pageable
    ): Page<JobSummaryView> {
        // 1截뤴깵 QueryDSL 湲곕컲 Entity 議고쉶
        val projectionPage = queryDslRepository.search(
            brandId = condition.brandId,
            positionId = condition.positionId,
            keyword = condition.keyword,
            pageable = pageable
        )

        // 2截뤴깵 Entity ??Read Model(View) 蹂????諛섑솚
        return PageImpl(
            projectionPage.content.map { it.toSummaryView() }, // ??Projection ??View
            pageable,
            projectionPage.totalElements
        )
    }

    /**
     * JobSummary ?곸꽭 議고쉶 (?ъ슜??????곹깭 ?ы븿)
     *
     * ?먮쫫:
     * 1. QueryDSL Projection?쇰줈 JobSummary 議고쉶
     * 2. ?꾩옱 ?ъ슜?먯쓽 MemberJobSummary 議고쉶
     * 3. copy()濡??⑹궛
     *
     * ?ㅺ퀎:
     * - Review??蹂꾨룄 API(/api/job-summary/review/{jobSummaryId})濡?遺꾨━
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
     * URL 以묐났 泥댄겕 (?쒖꽦?붾맂 寃껊쭔)
     *
     * ?뺤콉:
     * - 鍮꾪솢?깊솕??URL? ?덈줈 ?앹꽦 媛??     */
    override fun existsBySourceUrl(sourceUrl: String): Boolean {
        return jpaRepository.existsBySourceUrlAndIsActiveTrue(sourceUrl)
    }

    override fun existsByJobSnapshotId(jobSnapshotId: Long): Boolean {
        return jpaRepository.existsByJobSnapshotIdAndIsActiveTrue(jobSnapshotId)
    }

    override fun findIdByJobSnapshotId(jobSnapshotId: Long): Long? {
        return jpaRepository.findByJobSnapshotIdAndIsActiveTrue(jobSnapshotId)?.id
    }

    override fun findDetailByIdAdmin(jobSummaryId: Long): JobSummaryDetailView? {
        return queryDslRepository.findDetailByIdAdmin(jobSummaryId)
    }

    override fun searchAdmin(isActive: Boolean?, brandName: String?, pageable: Pageable): Page<JobSummaryAdminView> {
        return queryDslRepository.searchAdmin(isActive, brandName, pageable)
    }

    /**
     * URL濡?議고쉶 (?쒖꽦?붾맂 寃껊쭔)
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

