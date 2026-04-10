package com.hirelog.api.job.presentation.controller.dto.response

import com.hirelog.api.job.application.summary.view.JobSummaryReviewView
import java.time.LocalDateTime

data class JobSummaryReviewAdminRes(
    val id: Long,
    val jobSummaryId: Long,
    val brandName: String?,
    val brandPositionName: String?,
    val anonymous: Boolean,
    val memberId: Long?,
    val memberName: String?,
    val hiringStage: String,
    val difficultyRating: Int,
    val satisfactionRating: Int,
    val prosComment: String,
    val consComment: String,
    val tip: String?,
    val likeCount: Long,
    val deleted: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(view: JobSummaryReviewView): JobSummaryReviewAdminRes =
            JobSummaryReviewAdminRes(
                id = view.reviewId,
                jobSummaryId = view.jobSummaryId,
                brandName = view.brandName,
                brandPositionName = view.brandPositionName,
                anonymous = view.anonymous,
                memberId = view.memberId,
                memberName = view.memberName,
                hiringStage = view.hiringStage.name,
                difficultyRating = view.difficultyRating,
                satisfactionRating = view.satisfactionRating,
                prosComment = view.prosComment,
                consComment = view.consComment,
                tip = view.tip,
                likeCount = view.likeCount,
                deleted = view.deleted,
                createdAt = view.createdAt
            )
    }
}
