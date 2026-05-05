package com.hirelog.api.job.infra.persistence.jpa

import com.hirelog.api.job.application.rag.port.RagCohortQuery
import com.hirelog.api.job.application.rag.port.RagReviewRecord
import com.hirelog.api.job.application.rag.port.RagStageRecord
import com.hirelog.api.job.domain.model.QJobSummary.jobSummary
import com.hirelog.api.job.domain.model.QJobSummaryReview.jobSummaryReview
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.domain.model.QHiringStageRecord.hiringStageRecord
import com.hirelog.api.relation.domain.model.QMemberJobSummary.memberJobSummary
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * RagCohortQuery JPA 어댑터
 *
 * 책임:
 * - STATISTICS: saveType / stage / stageResult 기반 jobSummaryId 목록 조회
 * - EXPERIENCE_ANALYSIS: 사용자 면접 경험 기록 조회
 */
@Component
@Transactional(readOnly = true)
class RagCohortQueryJpaAdapter(
    private val queryFactory: JPAQueryFactory
) : RagCohortQuery {

    /**
     * cohort 조건에 해당하는 jobSummaryId 목록 반환
     *
     * - saveType: MemberJobSummary 필터 (SAVED/APPLY)
     * - stage / stageResult: HiringStageRecord 조건 (LEFT JOIN → EXISTS 방식)
     */
    override fun findJobSummaryIdsByCohort(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        stage: HiringStage?,
        stageResult: HiringStageResult?
    ): List<Long> {
        val hasStageFilter = stage != null || stageResult != null

        return if (hasStageFilter) {
            queryFactory
                .selectDistinct(memberJobSummary.jobSummaryId)
                .from(memberJobSummary)
                .join(hiringStageRecord)
                .on(hiringStageRecord.memberJobSummaryId.eq(memberJobSummary.id))
                .where(
                    memberJobSummary.memberId.eq(memberId),
                    memberJobSummary.saveType.ne(MemberJobSummarySaveType.UNSAVED),
                    saveType?.let { memberJobSummary.saveType.eq(it) },
                    stage?.let { hiringStageRecord.stage.eq(it) },
                    stageResult?.let { hiringStageRecord.result.eq(it) }
                )
                .fetch()
        } else {
            queryFactory
                .selectDistinct(memberJobSummary.jobSummaryId)
                .from(memberJobSummary)
                .where(
                    memberJobSummary.memberId.eq(memberId),
                    memberJobSummary.saveType.ne(MemberJobSummarySaveType.UNSAVED),
                    saveType?.let { memberJobSummary.saveType.eq(it) }
                )
                .fetch()
        }
    }

    /**
     * 면접/전형 경험 기록 조회
     *
     * HiringStageRecord + MemberJobSummary(brandName, positionName 스냅샷) 조인
     */
    override fun findStageRecordsForRag(
        memberId: Long,
        stage: HiringStage?,
        stageResult: HiringStageResult?
    ): List<RagStageRecord> {
        return queryFactory
            .select(
                memberJobSummary.brandName,
                memberJobSummary.positionName,
                hiringStageRecord.stage,
                hiringStageRecord.note,
                hiringStageRecord.result
            )
            .from(hiringStageRecord)
            .join(memberJobSummary)
            .on(memberJobSummary.id.eq(hiringStageRecord.memberJobSummaryId))
            .where(
                memberJobSummary.memberId.eq(memberId),
                stage?.let { hiringStageRecord.stage.eq(it) },
                stageResult?.let { hiringStageRecord.result.eq(it) }
            )
            .orderBy(hiringStageRecord.recordedAt.desc())
            .fetch()
            .map { tuple ->
                RagStageRecord(
                    brandName = tuple[memberJobSummary.brandName]!!,
                    positionName = tuple[memberJobSummary.positionName]!!,
                    stage = tuple[hiringStageRecord.stage]!!.name,
                    note = tuple[hiringStageRecord.note]!!,
                    result = tuple[hiringStageRecord.result]?.name
                )
            }
    }

    override fun findReviewsByMemberId(memberId: Long): List<RagReviewRecord> {
        return queryFactory
            .select(
                jobSummary.brandName,
                jobSummary.positionName,
                jobSummaryReview.hiringStage,
                jobSummaryReview.difficultyRating,
                jobSummaryReview.satisfactionRating,
                jobSummaryReview.prosComment,
                jobSummaryReview.consComment,
                jobSummaryReview.tip
            )
            .from(jobSummaryReview)
            .join(jobSummary).on(jobSummary.id.eq(jobSummaryReview.jobSummaryId))
            .where(
                jobSummaryReview.memberId.eq(memberId),
                jobSummaryReview.deleted.isFalse
            )
            .orderBy(jobSummaryReview.id.desc())
            .fetch()
            .map { tuple ->
                RagReviewRecord(
                    brandName = tuple[jobSummary.brandName]!!,
                    positionName = tuple[jobSummary.positionName]!!,
                    hiringStage = tuple[jobSummaryReview.hiringStage]!!.name,
                    difficultyRating = tuple[jobSummaryReview.difficultyRating]!!,
                    satisfactionRating = tuple[jobSummaryReview.satisfactionRating]!!,
                    prosComment = tuple[jobSummaryReview.prosComment]!!,
                    consComment = tuple[jobSummaryReview.consComment]!!,
                    tip = tuple[jobSummaryReview.tip]
                )
            }
    }
}
