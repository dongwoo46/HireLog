package com.hirelog.api.job.application.summary

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.review.port.JobSummaryReviewQuery
import com.hirelog.api.job.application.summary.view.JobSummaryReviewView
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.job.presentation.controller.dto.response.JobSummaryReviewRes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class JobSummaryReviewReadService(
    private val query: JobSummaryReviewQuery
) {

    fun findByJobSummaryId(
        jobSummaryId: Long,
        hiringStage: HiringStage?,
        minDifficultyRating: Int?,
        maxDifficultyRating: Int?,
        minSatisfactionRating: Int?,
        maxSatisfactionRating: Int?,
        page: Int,
        size: Int
    ): PagedResult<JobSummaryReviewRes> {

        val result = query.findByJobSummaryId(
            jobSummaryId = jobSummaryId,
            hiringStage = hiringStage,
            minDifficultyRating = minDifficultyRating,
            maxDifficultyRating = maxDifficultyRating,
            minSatisfactionRating = minSatisfactionRating,
            maxSatisfactionRating = maxSatisfactionRating,
            page = page,
            size = size
        )

        return result.map { view -> maskAnonymous(view) }
    }

    private fun maskAnonymous(view: JobSummaryReviewView): JobSummaryReviewRes =
        JobSummaryReviewRes(
            id = view.reviewId,
            anonymous = view.anonymous,
            memberId = if (view.anonymous) null else view.memberId,
            memberName = if (view.anonymous) null else view.memberName,
            hiringStage = view.hiringStage.name,
            difficultyRating = view.difficultyRating,
            satisfactionRating = view.satisfactionRating,
            experienceComment = view.experienceComment,
            interviewTip = view.interviewTip,
            createdAt = view.createdAt
        )
}
