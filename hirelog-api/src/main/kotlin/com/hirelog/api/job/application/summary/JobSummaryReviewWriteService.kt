package com.hirelog.api.job.application.review

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.review.port.JobSummaryReviewCommand
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.job.domain.model.JobSummaryReview
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JobSummaryReview Writer Service
 *
 * 책임:
 * - 리뷰 작성 유스케이스 실행
 * - 리뷰 삭제 (admin 전용) 유스케이스 실행
 * - 트랜잭션 경계 정의
 */
@Service
class JobSummaryReviewWriteService(
    private val command: JobSummaryReviewCommand
) {

    /**
     * 리뷰 작성 (1회만 가능)
     *
     * 정책:
     * - 동일 JD + 동일 회원 리뷰는 1개만 존재
     * - 이미 존재하면 예외 발생
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

        log.info("[JobSummaryReview create 1]: anaymonus:{}", anonymous)

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

        return command.save(review)
    }

    /**
     * 리뷰 Soft Delete (admin 전용)
     *
     * 정책:
     * - 이미 삭제된 리뷰는 예외 발생
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
    }

    /**
     * 리뷰 Restore (admin 전용)
     *
     * 정책:
     * - 삭제된 리뷰만 복구 가능
     * - 삭제되지 않은 리뷰는 예외 발생
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
    }
}
