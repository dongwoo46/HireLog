package com.hirelog.api.relation.infra.persistence.jpa.adapter

import com.hirelog.api.job.domain.QJobSummary.jobSummary
import com.hirelog.api.relation.application.jobsummary.query.MemberJobSummaryQuery
import com.hirelog.api.relation.application.jobsummary.view.MemberJobSummaryView
import com.hirelog.api.relation.application.jobsummary.view.SavedJobSummaryView
import com.hirelog.api.relation.domain.model.QMemberJobSummary.memberJobSummary
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import com.hirelog.api.common.application.port.PagedResult
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class MemberJobSummaryJpaQuery(
    private val queryFactory: JPAQueryFactory
) : MemberJobSummaryQuery {

    override fun findAllByMemberId(
        memberId: Long,
        page: Int,
        size: Int
    ): PagedResult<MemberJobSummaryView> {

        val offset = page.toLong() * size

        val items = queryFactory
            .select(
                Projections.constructor(
                    MemberJobSummaryView::class.java,
                    memberJobSummary.memberId,
                    memberJobSummary.jobSummaryId,
                    memberJobSummary.saveType,
                    memberJobSummary.memo,
                    memberJobSummary.createdAt,
                    memberJobSummary.updatedAt
                )
            )
            .from(memberJobSummary)
            .where(memberJobSummary.memberId.eq(memberId))
            .orderBy(memberJobSummary.createdAt.desc())
            .offset(offset)
            .limit(size.toLong())
            .fetch()

        val totalElements = queryFactory
            .select(memberJobSummary.count())
            .from(memberJobSummary)
            .where(memberJobSummary.memberId.eq(memberId))
            .fetchOne() ?: 0L

        return PagedResult.of(
            items = items,
            page = page,
            size = size,
            totalElements = totalElements
        )
    }


    override fun existsByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean =
        queryFactory
            .selectOne()
            .from(memberJobSummary)
            .where(
                memberJobSummary.memberId.eq(memberId),
                memberJobSummary.jobSummaryId.eq(jobSummaryId)
            )
            .fetchFirst() != null

    override fun findSavedJobSummaries(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        page: Int,
        size: Int
    ): PagedResult<SavedJobSummaryView> {

        val offset = page.toLong() * size

        val items = queryFactory
            .select(
                Projections.constructor(
                    SavedJobSummaryView::class.java,
                    memberJobSummary.memberId,
                    memberJobSummary.jobSummaryId,
                    memberJobSummary.saveType,
                    memberJobSummary.memo,
                    memberJobSummary.createdAt,
                    jobSummary.brandId,
                    jobSummary.brandName,
                    jobSummary.companyId,
                    jobSummary.companyName,
                    jobSummary.positionId,
                    jobSummary.positionName,
                    jobSummary.brandPositionName,
                    jobSummary.careerType,
                    jobSummary.careerYears,
                    jobSummary.summaryText,
                    jobSummary.techStack
                )
            )
            .from(memberJobSummary)
            .join(jobSummary)
            .on(memberJobSummary.jobSummaryId.eq(jobSummary.id))
            .where(
                memberJobSummary.memberId.eq(memberId),
                saveTypeEq(saveType)
            )
            .orderBy(memberJobSummary.createdAt.desc())
            .offset(offset)
            .limit(size.toLong())
            .fetch()

        val totalElements = queryFactory
            .select(memberJobSummary.count())
            .from(memberJobSummary)
            .where(
                memberJobSummary.memberId.eq(memberId),
                saveTypeEq(saveType)
            )
            .fetchOne() ?: 0L

        return PagedResult.of(
            items = items,
            page = page,
            size = size,
            totalElements = totalElements
        )
    }


    private fun saveTypeEq(saveType: MemberJobSummarySaveType?): BooleanExpression? =
        saveType?.let { memberJobSummary.saveType.eq(it) }
}
