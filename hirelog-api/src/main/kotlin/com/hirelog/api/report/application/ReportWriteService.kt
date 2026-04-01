package com.hirelog.api.report.application

import com.hirelog.api.board.application.BoardWriteService
import com.hirelog.api.comment.application.CommentWriteService
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.JobSummaryWriteService
import com.hirelog.api.report.application.port.ReportCommand
import com.hirelog.api.report.domain.model.Report
import com.hirelog.api.report.domain.type.ReportReason
import com.hirelog.api.report.domain.type.ReportStatus
import com.hirelog.api.report.domain.type.ReportTargetType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReportWriteService(
    private val command: ReportCommand,
    private val boardWriteService: BoardWriteService,
    private val commentWriteService: CommentWriteService,
    private val jobSummaryWriteService: JobSummaryWriteService
) {

    enum class AdminProcessType {
        REVIEW,
        RESOLVE,
        RESOLVE_AND_DELETE_TARGET,
        REJECT
    }

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

    @Transactional
    fun processByAdmin(reportId: Long, adminMemberId: Long, processType: AdminProcessType) {
        when (processType) {
            AdminProcessType.REVIEW -> review(reportId = reportId, adminMemberId = adminMemberId)
            AdminProcessType.RESOLVE -> {
                val report = command.findById(reportId)
                    ?: throw IllegalArgumentException("신고를 찾을 수 없습니다: $reportId")
                if (report.status == ReportStatus.PENDING) {
                    report.review(adminMemberId)
                }
                if (report.status == ReportStatus.REVIEWED) {
                    report.resolve(adminMemberId)
                }
                command.save(report)
                log.info("[REPORT_PROCESSED_RESOLVE] id={}, adminMemberId={}", reportId, adminMemberId)
            }
            AdminProcessType.RESOLVE_AND_DELETE_TARGET ->
                resolveAndDeleteTarget(reportId = reportId, adminMemberId = adminMemberId)
            AdminProcessType.REJECT -> {
                val report = command.findById(reportId)
                    ?: throw IllegalArgumentException("신고를 찾을 수 없습니다: $reportId")
                if (report.status == ReportStatus.PENDING) {
                    report.review(adminMemberId)
                }
                if (report.status == ReportStatus.REVIEWED) {
                    report.reject(adminMemberId)
                }
                command.save(report)
                log.info("[REPORT_PROCESSED_REJECT] id={}, adminMemberId={}", reportId, adminMemberId)
            }
        }
    }

    @Transactional
    fun resolveAndDeleteTarget(reportId: Long, adminMemberId: Long) {
        val report = command.findById(reportId)
            ?: throw IllegalArgumentException("신고를 찾을 수 없습니다: $reportId")

        if (report.status == ReportStatus.PENDING) {
            report.review(adminMemberId)
        }

        when (report.targetType) {
            ReportTargetType.JOB_SUMMARY -> {
                val targetId = report.jobSummaryId ?: throw IllegalStateException("jobSummaryId is null")
                jobSummaryWriteService.deactivate(targetId)
            }
            ReportTargetType.BOARD -> {
                val targetId = report.boardId ?: throw IllegalStateException("boardId is null")
                boardWriteService.delete(
                    boardId = targetId,
                    requesterId = adminMemberId,
                    isAdmin = true,
                    guestPassword = null
                )
            }
            ReportTargetType.COMMENT -> {
                val targetId = report.commentId ?: throw IllegalStateException("commentId is null")
                commentWriteService.delete(
                    commentId = targetId,
                    requesterId = adminMemberId,
                    isAdmin = true,
                    guestPassword = null
                )
            }
            else -> throw IllegalArgumentException(
                "해당 신고 대상은 삭제 처리 API를 지원하지 않습니다: ${report.targetType}"
            )
        }

        if (report.status != ReportStatus.RESOLVED) {
            report.resolve(adminMemberId)
        }

        command.save(report)
        log.info(
            "[REPORT_RESOLVED_AND_TARGET_DELETED] id={}, targetType={}, adminMemberId={}",
            reportId, report.targetType, adminMemberId
        )
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
        require(!isDuplicate) { "이미 신고된 대상입니다" }
    }
}
