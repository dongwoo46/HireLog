package com.hirelog.api.report.infrastructure.adapter

import com.hirelog.api.report.application.port.ReportCommand
import com.hirelog.api.report.domain.model.Report
import com.hirelog.api.report.infrastructure.ReportJpaRepository
import org.springframework.stereotype.Component

@Component
class ReportJpaCommandAdapter(
    private val repository: ReportJpaRepository
) : ReportCommand {

    override fun save(report: Report): Report = repository.save(report)

    override fun findById(id: Long): Report? = repository.findById(id).orElse(null)

    override fun existsByReporterAndJobSummary(reporterId: Long, jobSummaryId: Long): Boolean =
        repository.existsByReporterIdAndJobSummaryId(reporterId, jobSummaryId)

    override fun existsByReporterAndJobSummaryReview(reporterId: Long, jobSummaryReviewId: Long): Boolean =
        repository.existsByReporterIdAndJobSummaryReviewId(reporterId, jobSummaryReviewId)

    override fun existsByReporterAndReportedMember(reporterId: Long, reportedMemberId: Long): Boolean =
        repository.existsByReporterIdAndReportedMemberId(reporterId, reportedMemberId)

    override fun existsByReporterAndBoard(reporterId: Long, boardId: Long): Boolean =
        repository.existsByReporterIdAndBoardId(reporterId, boardId)

    override fun existsByReporterAndComment(reporterId: Long, commentId: Long): Boolean =
        repository.existsByReporterIdAndCommentId(reporterId, commentId)
}
