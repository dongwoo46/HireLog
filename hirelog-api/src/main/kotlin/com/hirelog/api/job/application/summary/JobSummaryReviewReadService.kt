package com.hirelog.api.job.application.summary

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.review.port.JobSummaryReviewQuery
import com.hirelog.api.job.application.summary.view.JobSummaryReviewView
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.job.presentation.controller.dto.response.JobSummaryReviewRes
import com.hirelog.api.relation.application.memberjobsummary.port.MemberJobSummaryQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class JobSummaryReviewReadService(
    private val query: JobSummaryReviewQuery,
    private val memberJobSummaryQuery: MemberJobSummaryQuery
) {

    fun findByJobSummaryId(
        memberId: Long,
        jobSummaryId: Long,
        hiringStage: HiringStage?,
        minDifficultyRating: Int?,
        maxDifficultyRating: Int?,
        minSatisfactionRating: Int?,
        maxSatisfactionRating: Int?,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<JobSummaryReviewRes> {
        // 정책: 리뷰 조회는 최소 1건 이상의 "내 JD 등록 이력"이 있는 사용자만 가능
        require(memberJobSummaryQuery.existsAnyByMemberId(memberId)) {
            "리뷰 조회는 본인이 등록한 JD가 1개 이상 있을 때 가능합니다."
        }

        val result = query.findByJobSummaryId(
            jobSummaryId = jobSummaryId,
            hiringStage = hiringStage,
            minDifficultyRating = minDifficultyRating,
            maxDifficultyRating = maxDifficultyRating,
            minSatisfactionRating = minSatisfactionRating,
            maxSatisfactionRating = maxSatisfactionRating,
            includeDeleted = includeDeleted,
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
            deleted = view.deleted,
            createdAt = view.createdAt
        )
}
