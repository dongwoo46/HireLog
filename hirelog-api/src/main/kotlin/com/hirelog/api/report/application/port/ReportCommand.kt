package com.hirelog.api.report.application.port

import com.hirelog.api.report.domain.model.Report

interface ReportCommand {
    fun save(report: Report): Report
    fun findById(id: Long): Report?
    fun existsByReporterAndJobSummary(reporterId: Long, jobSummaryId: Long): Boolean
    fun existsByReporterAndJobSummaryReview(reporterId: Long, jobSummaryReviewId: Long): Boolean
    fun existsByReporterAndReportedMember(reporterId: Long, reportedMemberId: Long): Boolean
    fun existsByReporterAndBoard(reporterId: Long, boardId: Long): Boolean
    fun existsByReporterAndComment(reporterId: Long, commentId: Long): Boolean
}
