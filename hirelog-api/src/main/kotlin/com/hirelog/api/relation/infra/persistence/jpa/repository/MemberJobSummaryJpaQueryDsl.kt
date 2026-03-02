package com.hirelog.api.relation.infra.persistence.jpa.repository

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.relation.application.memberjobsummary.view.*
import com.hirelog.api.relation.domain.model.QCoverLetter.coverLetter
import com.hirelog.api.relation.domain.model.QHiringStageRecord.hiringStageRecord
import com.hirelog.api.relation.domain.model.QMemberJobSummary.memberJobSummary
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class MemberJobSummaryJpaQueryDsl(
    private val queryFactory: JPAQueryFactory
) {

    fun findMySummaries(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        page: Int,
        size: Int
    ): PagedResult<MemberJobSummaryListView> {

        val tuples = queryFactory
            .select(
                memberJobSummary.id,
                memberJobSummary.jobSummaryId,
                memberJobSummary.brandName,
                memberJobSummary.positionName,
                memberJobSummary.brandPositionName,
                memberJobSummary.positionCategoryName,
                memberJobSummary.saveType,
                memberJobSummary.createdAt
            )
            .from(memberJobSummary)
            .where(
                memberJobSummary.memberId.eq(memberId),
                saveType?.let { memberJobSummary.saveType.eq(it) }
            )
            .orderBy(memberJobSummary.createdAt.desc())
            .offset((page * size).toLong())
            .limit(size.toLong())
            .fetch()

        val content = tuples.map { tuple ->
            MemberJobSummaryListView(
                memberJobSummaryId = tuple[memberJobSummary.id]!!,
                jobSummaryId = tuple[memberJobSummary.jobSummaryId]!!,
                brandName = tuple[memberJobSummary.brandName]!!,
                positionName = tuple[memberJobSummary.positionName]!!,
                brandPositionName = tuple[memberJobSummary.brandPositionName]!!,
                positionCategoryName = tuple[memberJobSummary.positionCategoryName]!!,
                saveType = tuple[memberJobSummary.saveType]!!,
                createdAt = tuple[memberJobSummary.createdAt]!!
            )
        }

        val total = queryFactory
            .select(memberJobSummary.count())
            .from(memberJobSummary)
            .where(
                memberJobSummary.memberId.eq(memberId),
                saveType?.let { memberJobSummary.saveType.eq(it) }
            )
            .fetchOne() ?: 0L

        return PagedResult.of(content, page, size, total)
    }

    fun findStages(
        memberId: Long,
        jobSummaryId: Long
    ): List<HiringStageView> {

        val memberJobSummaryId = queryFactory
            .select(memberJobSummary.id)
            .from(memberJobSummary)
            .where(
                memberJobSummary.memberId.eq(memberId),
                memberJobSummary.jobSummaryId.eq(jobSummaryId)
            )
            .fetchOne()
            ?: throw NoSuchElementException(
                "MemberJobSummary not found (memberId=$memberId, jobSummaryId=$jobSummaryId)"
            )

        return queryFactory
            .select(
                hiringStageRecord.stage,
                hiringStageRecord.note,
                hiringStageRecord.result,
                hiringStageRecord.recordedAt
            )
            .from(hiringStageRecord)
            .where(
                hiringStageRecord.memberJobSummaryId.eq(memberJobSummaryId)
            )
            .orderBy(hiringStageRecord.recordedAt.asc())
            .fetch()
            .map {
                HiringStageView(
                    stage = it[hiringStageRecord.stage]!!,
                    note = it[hiringStageRecord.note]!!,
                    result = it[hiringStageRecord.result],
                    recordedAt = it[hiringStageRecord.recordedAt]!!
                )
            }
    }

    fun exists(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean {
        return queryFactory
            .selectOne()
            .from(memberJobSummary)
            .where(
                memberJobSummary.memberId.eq(memberId),
                memberJobSummary.jobSummaryId.eq(jobSummaryId)
            )
            .fetchFirst() != null
    }

    fun findSavedStatesByJobSummaryIds(
        memberId: Long,
        jobSummaryIds: Set<Long>
    ): Map<Long, JobSummarySavedStateView> {
        if (jobSummaryIds.isEmpty()) return emptyMap()

        return queryFactory
            .select(
                memberJobSummary.id,
                memberJobSummary.jobSummaryId,
                memberJobSummary.saveType
            )
            .from(memberJobSummary)
            .where(
                memberJobSummary.memberId.eq(memberId),
                memberJobSummary.jobSummaryId.`in`(jobSummaryIds)
            )
            .fetch()
            .associate { tuple ->
                val jobSummaryId = tuple[memberJobSummary.jobSummaryId]!!
                jobSummaryId to JobSummarySavedStateView(
                    jobSummaryId = jobSummaryId,
                    memberJobSummaryId = tuple[memberJobSummary.id]!!,
                    saveType = tuple[memberJobSummary.saveType]!!
                )
            }
    }

    fun findCoverLetters(
        memberId: Long,
        jobSummaryId: Long
    ): List<CoverLetterView> {

        val memberJobSummaryId = queryFactory
            .select(memberJobSummary.id)
            .from(memberJobSummary)
            .where(
                memberJobSummary.memberId.eq(memberId),
                memberJobSummary.jobSummaryId.eq(jobSummaryId)
            )
            .fetchOne()
            ?: throw NoSuchElementException(
                "MemberJobSummary not found (memberId=$memberId, jobSummaryId=$jobSummaryId)"
            )

        return queryFactory
            .select(
                coverLetter.id,
                coverLetter.question,
                coverLetter.content,
                coverLetter.sortOrder
            )
            .from(coverLetter)
            .where(coverLetter.memberJobSummaryId.eq(memberJobSummaryId))
            .orderBy(coverLetter.sortOrder.asc())
            .fetch()
            .map {
                CoverLetterView(
                    id = it[coverLetter.id]!!,
                    question = it[coverLetter.question]!!,
                    content = it[coverLetter.content]!!,
                    sortOrder = it[coverLetter.sortOrder]!!
                )
            }
    }
}
