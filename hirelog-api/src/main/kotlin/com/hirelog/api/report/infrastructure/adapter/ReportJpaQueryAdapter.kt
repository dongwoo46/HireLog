package com.hirelog.api.report.infrastructure.adapter

import com.hirelog.api.board.domain.QBoard
import com.hirelog.api.comment.domain.QComment
import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.domain.model.QJobSummary
import com.hirelog.api.member.domain.QMember
import com.hirelog.api.report.application.port.ReportQuery
import com.hirelog.api.report.application.view.ReportView
import com.hirelog.api.report.domain.model.QReport
import com.hirelog.api.report.domain.type.ReportStatus
import com.hirelog.api.report.domain.type.ReportTargetType
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class ReportJpaQueryAdapter(
    private val queryFactory: JPAQueryFactory
) : ReportQuery {

    private val report = QReport.report
    private val reporter = QMember("reporter")
    private val reportedMember = QMember("reportedMember")
    private val jobSummary = QJobSummary.jobSummary
    private val board = QBoard.board
    private val comment = QComment.comment

    override fun findAll(
        status: ReportStatus?,
        targetType: ReportTargetType?,
        page: Int,
        size: Int
    ): PagedResult<ReportView> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..100) { "size must be between 1 and 100" }

        val condition = BooleanBuilder()
        status?.let { condition.and(report.status.eq(it)) }
        targetType?.let {
            when (it) {
                ReportTargetType.JOB_SUMMARY -> condition.and(report.jobSummaryId.isNotNull)
                ReportTargetType.JOB_SUMMARY_REVIEW -> condition.and(report.jobSummaryReviewId.isNotNull)
                ReportTargetType.MEMBER -> condition.and(report.reportedMemberId.isNotNull)
                ReportTargetType.BOARD -> condition.and(report.boardId.isNotNull)
                ReportTargetType.COMMENT -> condition.and(report.commentId.isNotNull)
            }
        }

        val totalElements = queryFactory
            .select(report.id.count())
            .from(report)
            .where(condition)
            .fetchOne() ?: 0L

        val items = queryFactory
            .select(
                Projections.constructor(
                    ReportView::class.java,
                    report.id,
                    report.reporterId,
                    reporter.username,
                    report.jobSummaryId,
                    jobSummary.brandPositionName,
                    report.jobSummaryReviewId,
                    report.reportedMemberId,
                    reportedMember.username,
                    report.boardId,
                    board.title,
                    report.commentId,
                    comment.content,
                    report.reason,
                    report.detail,
                    report.status,
                    report.reviewedAt,
                    report.createdAt
                )
            )
            .from(report)
            .leftJoin(reporter).on(report.reporterId.eq(reporter.id))
            .leftJoin(jobSummary).on(report.jobSummaryId.eq(jobSummary.id))
            .leftJoin(reportedMember).on(report.reportedMemberId.eq(reportedMember.id))
            .leftJoin(board).on(report.boardId.eq(board.id))
            .leftJoin(comment).on(report.commentId.eq(comment.id))
            .where(condition)
            .orderBy(report.id.desc())
            .offset((page * size).toLong())
            .limit(size.toLong())
            .fetch()

        return PagedResult.of(
            items = items,
            page = page,
            size = size,
            totalElements = totalElements
        )
    }
}
