package com.hirelog.api.job.application.review

import com.hirelog.api.job.application.review.port.JobSummaryReviewCommand
import com.hirelog.api.job.application.review.port.JobSummaryReviewQuery
import com.hirelog.api.job.domain.HiringStage
import com.hirelog.api.job.domain.JobSummaryReview
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JobSummaryReview Writer Service
 *
 * 책임:
 * - 리뷰 작성/수정 유스케이스 실행
 * - 트랜잭션 경계 정의
 */
@Service
class JobSummaryReviewWriteService(
    private val command: JobSummaryReviewCommand,
    private val query: JobSummaryReviewQuery
) {

    /**
     * 리뷰 작성 또는 수정
     *
     * 정책:
     * - 동일 JD + 동일 회원 리뷰는 1개만 존재
     * - 존재하면 수정, 없으면 신규 생성
     */
    @Transactional
    fun write(
        jobSummaryId: Long,
        memberId: Long,
        hiringStage: HiringStage,
        anonymous: Boolean,
        difficultyRating: Int,
        satisfactionRating: Int,
        experienceComment: String,
        interviewTip: String?
    ): JobSummaryReview {

        val existing = query.findByJobSummaryIdAndMemberId(
            jobSummaryId = jobSummaryId,
            memberId = memberId
        )

        val review =
            if (existing == null) {
                JobSummaryReview.create(
                    jobSummaryId = jobSummaryId,
                    memberId = memberId,
                    hiringStage = hiringStage,
                    anonymous = anonymous,
                    difficultyRating = difficultyRating,
                    satisfactionRating = satisfactionRating,
                    experienceComment = experienceComment,
                    interviewTip = interviewTip
                )
            } else {
                existing.update(
                    hiringStage = hiringStage,
                    anonymous = anonymous,
                    difficultyRating = difficultyRating,
                    satisfactionRating = satisfactionRating,
                    experienceComment = experienceComment,
                    interviewTip = interviewTip
                )
                existing
            }

        return command.save(review)
    }
}
