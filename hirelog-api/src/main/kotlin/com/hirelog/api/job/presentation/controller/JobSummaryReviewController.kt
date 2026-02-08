package com.hirelog.api.job.presentation.controller.review

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.job.application.review.JobSummaryReviewWriteService
import com.hirelog.api.job.application.review.port.JobSummaryReviewQuery
import com.hirelog.api.job.application.review.port.PagedView
import com.hirelog.api.job.presentation.controller.dto.request.JobSummaryReviewWriteReq
import com.hirelog.api.job.presentation.controller.dto.response.JobSummaryReviewRes
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * JobSummaryReview Controller
 *
 * 책임:
 * - 리뷰 작성/조회 HTTP API 제공
 * - jobSummaryId는 메서드 단위에서만 사용
 */
@RestController
@RequestMapping("/api/job-summaries/review")
class JobSummaryReviewController(
    private val writerService: JobSummaryReviewWriteService,
    private val query: JobSummaryReviewQuery
) {

    /**
     * 리뷰 작성 또는 수정
     *
     * POST /job-summaries/review/{jobSummaryId}
     */
    @PostMapping("/{jobSummaryId}")
    fun writeReview(
        @PathVariable jobSummaryId: Long,
        @AuthenticationPrincipal member: AuthenticatedMember,
        @RequestBody @Valid request: JobSummaryReviewWriteReq
    ): ResponseEntity<JobSummaryReviewRes> {

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

        return ResponseEntity.ok(
            JobSummaryReviewRes.from(review)
        )
    }

    /**
     * 특정 JD 리뷰 목록 조회
     *
     * GET /job-summaries/review/{jobSummaryId}
     */
    @GetMapping("/{jobSummaryId}")
    fun getReviewsByJobSummary(
        @PathVariable jobSummaryId: Long
    ): ResponseEntity<List<JobSummaryReviewRes>> {

        val reviews =
            query.findAllByJobSummaryId(jobSummaryId)
                .map(JobSummaryReviewRes::from)

        return ResponseEntity.ok(reviews)
    }

    /**
     * 전체 리뷰 페이징 조회 (관리자)
     *
     * GET /job-summaries/review/admin
     */
    @GetMapping("/admin")
    fun getAllReviewsPaged(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedView<JobSummaryReviewRes>> {

        val result = query.findAllPaged(page, size)

        return ResponseEntity.ok(
            PagedView(
                items = result.items.map(JobSummaryReviewRes::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                hasNext = result.hasNext
            )
        )
    }
}
