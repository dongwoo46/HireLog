package com.hirelog.api.job.application.review

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.review.port.JobSummaryReviewCommand
import com.hirelog.api.job.domain.model.JobSummaryReview
import com.hirelog.api.job.domain.type.HiringStage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JobSummaryReview 쓰기 서비스
 *
 * 책임:
 * - 리뷰 작성
 * - 리뷰 수정/삭제/복구(관리자)
 */
@Service
class JobSummaryReviewWriteService(
    private val command: JobSummaryReviewCommand
) {

    /**
     * 리뷰 작성 (공고당 사용자 1개)
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

        val existing = command.findByJobSummaryIdAndMemberId(
            jobSummaryId = jobSummaryId,
            memberId = memberId
        )

        require(existing == null) {
            "이미 해당 공고에 리뷰를 작성했습니다."
        }

        val review = JobSummaryReview.create(
            jobSummaryId = jobSummaryId,
            memberId = memberId,
            hiringStage = hiringStage,
            anonymous = anonymous,
            difficultyRating = difficultyRating,
            satisfactionRating = satisfactionRating,
            experienceComment = experienceComment,
            interviewTip = interviewTip
        )

        val saved = command.save(review)

        log.info(
            "[JOB_SUMMARY_REVIEW_WRITTEN] jobSummaryId={}, memberId={}, reviewId={}, anonymous={}",
            jobSummaryId, memberId, saved.id, anonymous
        )

        return saved
    }

    /**
     * 리뷰 삭제 (관리자, soft delete)
     */
    @Transactional
    fun delete(reviewId: Long) {
        val review = command.findById(reviewId)
            ?: throw IllegalArgumentException("리뷰를 찾을 수 없습니다: $reviewId")

        require(!review.isDeleted()) {
            "이미 삭제된 리뷰입니다: $reviewId"
        }

        review.softDelete()
        command.save(review)

        log.info("[JOB_SUMMARY_REVIEW_DELETED] reviewId={}", reviewId)
    }

    /**
     * 리뷰 수정 (관리자)
     */
    @Transactional
    fun update(
        reviewId: Long,
        hiringStage: HiringStage,
        anonymous: Boolean,
        difficultyRating: Int,
        satisfactionRating: Int,
        experienceComment: String,
        interviewTip: String?
    ) {
        val review = command.findById(reviewId)
            ?: throw IllegalArgumentException("리뷰를 찾을 수 없습니다: $reviewId")

        require(!review.isDeleted()) {
            "삭제된 리뷰는 수정할 수 없습니다: $reviewId"
        }

        review.update(
            hiringStage = hiringStage,
            anonymous = anonymous,
            difficultyRating = difficultyRating,
            satisfactionRating = satisfactionRating,
            experienceComment = experienceComment,
            interviewTip = interviewTip
        )
        command.save(review)

        log.info("[JOB_SUMMARY_REVIEW_UPDATED] reviewId={}", reviewId)
    }

    /**
     * 리뷰 복구 (관리자)
     */
    @Transactional
    fun restore(reviewId: Long) {
        val review = command.findById(reviewId)
            ?: throw IllegalArgumentException("리뷰를 찾을 수 없습니다: $reviewId")

        require(review.isDeleted()) {
            "삭제되지 않은 리뷰는 복구할 수 없습니다: $reviewId"
        }

        review.restore()
        command.save(review)

        log.info("[JOB_SUMMARY_REVIEW_RESTORED] reviewId={}", reviewId)
    }
}