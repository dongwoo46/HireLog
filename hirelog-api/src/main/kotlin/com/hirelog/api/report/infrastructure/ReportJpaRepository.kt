package com.hirelog.api.report.infrastructure

import com.hirelog.api.report.domain.model.Report
import org.springframework.data.jpa.repository.JpaRepository

interface ReportJpaRepository : JpaRepository<Report, Long> {
    fun existsByReporterIdAndJobSummaryId(reporterId: Long, jobSummaryId: Long): Boolean
    fun existsByReporterIdAndJobSummaryReviewId(reporterId: Long, jobSummaryReviewId: Long): Boolean
    fun existsByReporterIdAndReportedMemberId(reporterId: Long, reportedMemberId: Long): Boolean
    fun existsByReporterIdAndBoardId(reporterId: Long, boardId: Long): Boolean
    fun existsByReporterIdAndCommentId(reporterId: Long, commentId: Long): Boolean
}
