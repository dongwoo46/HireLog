package com.hirelog.api.job.presentation.controller

import com.hirelog.api.job.application.summary.query.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummaryView
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/job-summary")
class JobSummaryController(
    private val jobSummaryQuery: JobSummaryQuery
) {

    /**
     * JobSummary 최신 1건 조회 (임시)
     *
     * 조회 정책:
     * - brandId, positionId 둘 다 없으면 전체 중 최신 1건
     * - 하나만 있으면 해당 조건 기준 최신 1건
     * - 둘 다 있으면 해당 조합 기준 최신 1건
     */
    @GetMapping("/latest")
    fun getLatest(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false) positionId: Long?
    ): ResponseEntity<JobSummaryView> {

        val result = jobSummaryQuery
            .search(
                brandId = brandId,
                positionId = positionId,
                keyword = null,
                page = 0,
                size = 10
            )
            .content
            .firstOrNull()
            ?: throw IllegalArgumentException("JobSummary not found")

        return ResponseEntity.ok(result)
    }
}
