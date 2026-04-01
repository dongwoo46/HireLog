package com.hirelog.api.report.presentation.controller.dto.response

import com.hirelog.api.report.application.view.ReportView
import com.hirelog.api.report.domain.type.ReportReason
import com.hirelog.api.report.domain.type.ReportStatus
import com.hirelog.api.report.domain.type.ReportTargetType
import java.time.LocalDateTime

data class ReportRes(
    val id: Long,
    val reporterId: Long,
    val reporterUsername: String,
    val targetType: ReportTargetType,
    val targetId: Long,
    val targetLabel: String?,           // brandPositionName 또는 reportedMemberUsername
    val reason: ReportReason,
    val detail: String?,
    val status: ReportStatus,
    val reviewedAt: LocalDateTime?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(view: ReportView) = ReportRes(
            id = view.id,
            reporterId = view.reporterId,
            reporterUsername = view.reporterUsername,
            targetType = view.targetType,
            targetId = view.targetId,
            targetLabel = view.brandPositionName ?: view.reportedMemberUsername
                ?: view.boardTitle ?: view.commentSnippet,
            reason = view.reason,
            detail = view.detail,
            status = view.status,
            reviewedAt = view.reviewedAt,
            createdAt = view.createdAt
        )
    }
}
