package com.hirelog.api.job.presentation.controller

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.review.JobSummaryReviewWriteService
import com.hirelog.api.job.application.summary.JobSummaryReviewReadService
import com.hirelog.api.job.presentation.controller.dto.request.JobSummaryReviewSearchReq
import com.hirelog.api.job.presentation.controller.dto.request.JobSummaryReviewWriteReq
import com.hirelog.api.job.presentation.controller.dto.response.JobSummaryReviewRes
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * JobSummary 리뷰 API
 */
@RestController
@RequestMapping("/api/job-summary/review")
class JobSummaryReviewController(
    private val writerService: JobSummaryReviewWriteService,
    private val readService: JobSummaryReviewReadService
) {

    /**
     * 리뷰 작성
     * POST /api/job-summary/review/{jobSummaryId}
     */
    @PostMapping("/{jobSummaryId}")
    fun writeReview(
        @PathVariable jobSummaryId: Long,
        @AuthenticationPrincipal member: AuthenticatedMember,
        @RequestBody @Valid request: JobSummaryReviewWriteReq
    ): ResponseEntity<Map<String, Long>> {

        log.info("[JOB_SUMMARY_REVIEW_CREATE] anonymous={}", request.anonymous)

        val review = writerService.write(
            jobSummaryId = jobSummaryId,
            memberId = member.memberId,
            hiringStage = request.hiringStage,
            anonymous = request.anonymous,
            difficultyRating = request.difficultyRating,
            satisfactionRating = request.satisfactionRating,
            experienceComment = request.experienceComment,
            interviewTip = request.interviewTip
        )

        return ResponseEntity.status(201).body(mapOf("id" to review.id))
    }

    /**
     * 리뷰 목록 조회
     * GET /api/job-summary/review/{jobSummaryId}
     */
    @GetMapping("/{jobSummaryId}")
    fun getReviewsByJobSummary(
        @PathVariable jobSummaryId: Long,
        @AuthenticationPrincipal member: AuthenticatedMember?,
        @Valid request: JobSummaryReviewSearchReq
    ): ResponseEntity<PagedResult<JobSummaryReviewRes>> {

        request.validate()
        val includeDeleted = request.includeDeleted && member?.role?.name == "ADMIN"

        val result = readService.findByJobSummaryId(
            jobSummaryId = jobSummaryId,
            hiringStage = request.hiringStage,
            minDifficultyRating = request.minDifficultyRating,
            maxDifficultyRating = request.maxDifficultyRating,
            minSatisfactionRating = request.minSatisfactionRating,
            maxSatisfactionRating = request.maxSatisfactionRating,
            includeDeleted = includeDeleted,
            page = request.page,
            size = request.size
        )

        return ResponseEntity.ok(result)
    }

    /**
     * 리뷰 삭제 (관리자)
     * DELETE /api/job-summary/review/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteReview(@PathVariable reviewId: Long): ResponseEntity<Void> {
        writerService.delete(reviewId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 리뷰 수정 (관리자)
     * PATCH /api/job-summary/review/{reviewId}
     */
    @PatchMapping("/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateReview(
        @PathVariable reviewId: Long,
        @RequestBody @Valid request: JobSummaryReviewWriteReq
    ): ResponseEntity<Void> {
        writerService.update(
            reviewId = reviewId,
            hiringStage = request.hiringStage,
            anonymous = request.anonymous,
            difficultyRating = request.difficultyRating,
            satisfactionRating = request.satisfactionRating,
            experienceComment = request.experienceComment,
            interviewTip = request.interviewTip
        )
        return ResponseEntity.noContent().build()
    }

    /**
     * 리뷰 복구 (관리자)
     * PATCH /api/job-summary/review/{reviewId}/restore
     */
    @PatchMapping("/{reviewId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    fun restoreReview(@PathVariable reviewId: Long): ResponseEntity<Void> {
        writerService.restore(reviewId)
        return ResponseEntity.noContent().build()
    }
}