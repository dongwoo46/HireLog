package com.hirelog.api.report.application.view

import com.hirelog.api.report.domain.type.ReportReason
import com.hirelog.api.report.domain.type.ReportStatus
import com.hirelog.api.report.domain.type.ReportTargetType
import java.time.LocalDateTime

data class ReportView(
    val id: Long,
    val reporterId: Long,
    val reporterUsername: String,
    val jobSummaryId: Long?,
    val brandPositionName: String?,       // JOB_SUMMARY 대상일 때만 non-null
    val jobSummaryReviewId: Long?,
    val reportedMemberId: Long?,
    val reportedMemberUsername: String?,  // MEMBER 대상일 때만 non-null
    val boardId: Long?,
    val boardTitle: String?,              // BOARD 대상일 때만 non-null
    val commentId: Long?,
    val commentSnippet: String?,          // COMMENT 대상일 때만 non-null
    val reason: ReportReason,
    val detail: String?,
    val status: ReportStatus,
    val reviewedAt: LocalDateTime?,
    val createdAt: LocalDateTime
) {
    val targetType: ReportTargetType
        get() = when {
            jobSummaryId != null -> ReportTargetType.JOB_SUMMARY
            jobSummaryReviewId != null -> ReportTargetType.JOB_SUMMARY_REVIEW
            reportedMemberId != null -> ReportTargetType.MEMBER
            boardId != null -> ReportTargetType.BOARD
            commentId != null -> ReportTargetType.COMMENT
            else -> throw IllegalStateException("신고 대상이 지정되지 않았습니다")
        }

    val targetId: Long
        get() = jobSummaryId ?: jobSummaryReviewId ?: reportedMemberId ?: boardId ?: commentId
            ?: throw IllegalStateException("신고 대상이 지정되지 않았습니다")
}