package com.hirelog.api.report.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.report.domain.type.ReportReason
import com.hirelog.api.report.domain.type.ReportStatus
import com.hirelog.api.report.domain.type.ReportTargetType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "report",
    indexes = [
        Index(name = "idx_report_status_created", columnList = "status, created_at"),
        Index(name = "idx_report_reporter", columnList = "reporter_id"),
        Index(name = "idx_report_job_summary", columnList = "job_summary_id"),
        Index(name = "idx_report_review", columnList = "job_summary_review_id"),
        Index(name = "idx_report_member", columnList = "reported_member_id"),
        Index(name = "idx_report_board", columnList = "board_id"),
        Index(name = "idx_report_comment", columnList = "comment_id")
    ]
)
class Report protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "reporter_id", nullable = false, updatable = false)
    val reporterId: Long,

    // nullable FK 3개 — targetType에 따라 하나만 채워짐
    // DB 레벨 FK constraint 적용 가능 (각 컬럼별 독립 FK)
    // 중복 신고 방지는 partial unique index로 처리 (Flyway 마이그레이션 필요)
    // CREATE UNIQUE INDEX ON report(reporter_id, job_summary_id) WHERE job_summary_id IS NOT NULL
    // CREATE UNIQUE INDEX ON report(reporter_id, job_summary_review_id) WHERE job_summary_review_id IS NOT NULL
    // CREATE UNIQUE INDEX ON report(reporter_id, reported_member_id) WHERE reported_member_id IS NOT NULL

    @Column(name = "job_summary_id", updatable = false)
    val jobSummaryId: Long? = null,

    @Column(name = "job_summary_review_id", updatable = false)
    val jobSummaryReviewId: Long? = null,

    @Column(name = "reported_member_id", updatable = false)
    val reportedMemberId: Long? = null,

    @Column(name = "board_id", updatable = false)
    val boardId: Long? = null,

    @Column(name = "comment_id", updatable = false)
    val commentId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30, updatable = false)
    val reason: ReportReason,

    @Column(name = "detail", columnDefinition = "TEXT", updatable = false)
    val detail: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ReportStatus = ReportStatus.PENDING,

    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null,

    @Column(name = "reviewed_by")
    var reviewedBy: Long? = null

) : BaseEntity() {

    val targetType: ReportTargetType
        get() = when {
            jobSummaryId != null -> ReportTargetType.JOB_SUMMARY
            jobSummaryReviewId != null -> ReportTargetType.JOB_SUMMARY_REVIEW
            reportedMemberId != null -> ReportTargetType.MEMBER
            boardId != null -> ReportTargetType.BOARD
            commentId != null -> ReportTargetType.COMMENT
            else -> throw IllegalStateException("신고 대상이 지정되지 않았습니다")
        }

    fun review(adminMemberId: Long) {
        require(status == ReportStatus.PENDING) { "PENDING 상태의 신고만 검토할 수 있습니다" }
        status = ReportStatus.REVIEWED
        reviewedAt = LocalDateTime.now()
        reviewedBy = adminMemberId
    }

    fun resolve(adminMemberId: Long) {
        require(status == ReportStatus.REVIEWED) { "REVIEWED 상태의 신고만 처리할 수 있습니다" }
        status = ReportStatus.RESOLVED
        reviewedAt = LocalDateTime.now()
        reviewedBy = adminMemberId
    }

    fun reject(adminMemberId: Long) {
        require(status == ReportStatus.REVIEWED) { "REVIEWED 상태의 신고만 반려할 수 있습니다" }
        status = ReportStatus.REJECTED
        reviewedAt = LocalDateTime.now()
        reviewedBy = adminMemberId
    }

    companion object {
        fun create(
            reporterId: Long,
            jobSummaryId: Long? = null,
            jobSummaryReviewId: Long? = null,
            reportedMemberId: Long? = null,
            boardId: Long? = null,
            commentId: Long? = null,
            reason: ReportReason,
            detail: String?
        ): Report {
            val filled = listOfNotNull(jobSummaryId, jobSummaryReviewId, reportedMemberId, boardId, commentId)
            require(filled.size == 1) { "신고 대상은 정확히 1개여야 합니다" }
            require(detail == null || detail.length <= 1000) { "신고 상세 내용은 1000자를 초과할 수 없습니다" }

            return Report(
                reporterId = reporterId,
                jobSummaryId = jobSummaryId,
                jobSummaryReviewId = jobSummaryReviewId,
                reportedMemberId = reportedMemberId,
                boardId = boardId,
                commentId = commentId,
                reason = reason,
                detail = detail
            )
        }
    }
}