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
 * JobSummaryReview Controller
 *
 * 책임:
 * - 리뷰 작성/조회/삭제 HTTP API 제공
 */
@RestController
@RequestMapping("/api/job-summary/review")
class JobSummaryReviewController(
    private val writerService: JobSummaryReviewWriteService,
    private val readService: JobSummaryReviewReadService
) {

    /**
     * 리뷰 작성 (1회만 가능, 수정 불가)
     *
     * POST /api/job-summary/review/{jobSummaryId}
     */
    @PostMapping("/{jobSummaryId}")
    fun writeReview(
        @PathVariable jobSummaryId: Long,
        @AuthenticationPrincipal member: AuthenticatedMember,
        @RequestBody @Valid request: JobSummaryReviewWriteReq
    ): ResponseEntity<Map<String, Long>> {

        log.info("[JobSummaryReview create 0]: anaymonus:{}", request.anonymous)

        val review =
            writerService.write(
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
     * 특정 JobSummary 리뷰 페이징 + 필터 조회
     *
     * GET /api/job-summary/review/{jobSummaryId}
     *
     * 필터:
     * - hiringStage: 전형 단계
     * - minDifficultyRating / maxDifficultyRating: 난이도 범위
     * - minSatisfactionRating / maxSatisfactionRating: 만족도 범위
     */
    @GetMapping("/{jobSummaryId}")
    fun getReviewsByJobSummary(
        @PathVariable jobSummaryId: Long,
        @Valid request: JobSummaryReviewSearchReq
    ): ResponseEntity<PagedResult<JobSummaryReviewRes>> {

        request.validate()

        val result = readService.findByJobSummaryId(
            jobSummaryId = jobSummaryId,
            hiringStage = request.hiringStage,
            minDifficultyRating = request.minDifficultyRating,
            maxDifficultyRating = request.maxDifficultyRating,
            minSatisfactionRating = request.minSatisfactionRating,
            maxSatisfactionRating = request.maxSatisfactionRating,
            page = request.page,
            size = request.size
        )

        return ResponseEntity.ok(result)
    }

    /**
     * 리뷰 삭제 (admin 전용, soft delete)
     *
     * DELETE /api/job-summary/review/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteReview(
        @PathVariable reviewId: Long
    ): ResponseEntity<Void> {
        writerService.delete(reviewId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 리뷰 복구 (admin 전용)
     *
     * PUT /api/job-summary/review/{reviewId}/restore
     */
    @PatchMapping("/{reviewId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    fun restoreReview(
        @PathVariable reviewId: Long
    ): ResponseEntity<Void> {
        writerService.restore(reviewId)
        return ResponseEntity.noContent().build()
    }
}
