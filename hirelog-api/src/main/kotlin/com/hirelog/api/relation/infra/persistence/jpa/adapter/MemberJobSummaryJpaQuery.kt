package com.hirelog.api.relation.infra.persistence.adapter

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.application.memberjobsummary.port.MemberJobSummaryQuery
import com.hirelog.api.relation.application.memberjobsummary.view.CoverLetterView
import com.hirelog.api.relation.application.memberjobsummary.view.HiringStageView
import com.hirelog.api.relation.application.memberjobsummary.view.JobSummarySavedStateView
import com.hirelog.api.relation.application.memberjobsummary.view.MemberJobSummaryListView
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberJobSummaryJpaQueryDsl
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * MemberJobSummary Query Adapter
 *
 * 책임:
 * - Read Port 구현
 * - Querydsl Repository 위임
 */
@Component
@Transactional(readOnly = true)
class MemberJobSummaryJpaQuery(
    private val querydslRepository: MemberJobSummaryJpaQueryDsl
) : MemberJobSummaryQuery {

    override fun findMySummaries(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        brandName: String?,
        stage: HiringStage?,
        stageResult: HiringStageResult?,
        page: Int,
        size: Int
    ): PagedResult<MemberJobSummaryListView> {
        return querydslRepository.findMySummaries(
            memberId = memberId,
            saveType = saveType,
            brandName = brandName,
            stage = stage,
            stageResult = stageResult,
            page = page,
            size = size
        )
    }

    override fun existsAnyByMemberId(memberId: Long): Boolean {
        return querydslRepository.existsAnyByMemberId(memberId)
    }

    override fun findStages(
        memberId: Long,
        jobSummaryId: Long
    ): List<HiringStageView> {
        return querydslRepository.findStages(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        )
    }

    override fun exists(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean {
        return querydslRepository.exists(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        )
    }

    override fun findCoverLetters(
        memberId: Long,
        jobSummaryId: Long
    ): List<CoverLetterView> {
        return querydslRepository.findCoverLetters(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        )
    }

    override fun findSavedStatesByJobSummaryIds(
        memberId: Long,
        jobSummaryIds: Set<Long>
    ): Map<Long, JobSummarySavedStateView> {
        return querydslRepository.findSavedStatesByJobSummaryIds(
            memberId = memberId,
            jobSummaryIds = jobSummaryIds
        )
    }

    override fun countSavedByJobSummaryIds(jobSummaryIds: Set<Long>): Map<Long, Long> {
        return querydslRepository.countSavedByJobSummaryIds(jobSummaryIds)
    }
}
