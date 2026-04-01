package com.hirelog.api.report.application

import com.hirelog.api.common.logging.log
import com.hirelog.api.report.application.port.ReportCommand
import com.hirelog.api.report.domain.model.Report
import com.hirelog.api.report.domain.type.ReportReason
import com.hirelog.api.report.domain.type.ReportTargetType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReportWriteService(
    private val command: ReportCommand
) {

    @Transactional
    fun report(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
        reason: ReportReason,
        detail: String?
    ): Report {
        checkDuplicate(reporterId, targetType, targetId)

        val report = when (targetType) {
            ReportTargetType.JOB_SUMMARY -> Report.create(
                reporterId = reporterId,
                jobSummaryId = targetId,
                reason = reason,
                detail = detail
            )
            ReportTargetType.JOB_SUMMARY_REVIEW -> Report.create(
                reporterId = reporterId,
                jobSummaryReviewId = targetId,
                reason = reason,
                detail = detail
            )
            ReportTargetType.MEMBER -> Report.create(
                reporterId = reporterId,
                reportedMemberId = targetId,
                reason = reason,
                detail = detail
            )
            ReportTargetType.BOARD -> Report.create(
                reporterId = reporterId,
                boardId = targetId,
                reason = reason,
                detail = detail
            )
            ReportTargetType.COMMENT -> Report.create(
                reporterId = reporterId,
                commentId = targetId,
                reason = reason,
                detail = detail
            )
        }

        val saved = command.save(report)
        log.info(
            "[REPORT_CREATED] id={}, reporterId={}, targetType={}, targetId={}",
            saved.id, reporterId, targetType, targetId
        )
        return saved
    }

    @Transactional
    fun review(reportId: Long, adminMemberId: Long) {
        val report = command.findById(reportId)
            ?: throw IllegalArgumentException("신고를 찾을 수 없습니다: $reportId")
        report.review(adminMemberId)
        command.save(report)
        log.info("[REPORT_REVIEWED] id={}, adminMemberId={}", reportId, adminMemberId)
    }

    @Transactional
    fun resolve(reportId: Long, adminMemberId: Long) {
        val report = command.findById(reportId)
            ?: throw IllegalArgumentException("신고를 찾을 수 없습니다: $reportId")
        report.resolve(adminMemberId)
        command.save(report)
        log.info("[REPORT_RESOLVED] id={}, adminMemberId={}", reportId, adminMemberId)
    }

    @Transactional
    fun reject(reportId: Long, adminMemberId: Long) {
        val report = command.findById(reportId)
            ?: throw IllegalArgumentException("신고를 찾을 수 없습니다: $reportId")
        report.reject(adminMemberId)
        command.save(report)
        log.info("[REPORT_REJECTED] id={}, adminMemberId={}", reportId, adminMemberId)
    }

    private fun checkDuplicate(reporterId: Long, targetType: ReportTargetType, targetId: Long) {
        val isDuplicate = when (targetType) {
            ReportTargetType.JOB_SUMMARY ->
                command.existsByReporterAndJobSummary(reporterId, targetId)
            ReportTargetType.JOB_SUMMARY_REVIEW ->
                command.existsByReporterAndJobSummaryReview(reporterId, targetId)
            ReportTargetType.MEMBER ->
                command.existsByReporterAndReportedMember(reporterId, targetId)
            ReportTargetType.BOARD ->
                command.existsByReporterAndBoard(reporterId, targetId)
            ReportTargetType.COMMENT ->
                command.existsByReporterAndComment(reporterId, targetId)
        }
        require(!isDuplicate) { "이미 신고한 대상입니다" }
    }
}
