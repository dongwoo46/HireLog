package com.hirelog.api.job.presentation.controller

import com.hirelog.api.job.application.preprocess.JdPreprocessRequestService
import com.hirelog.api.job.application.summary.JobSummaryGenerationFacadeService
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchCondition
import com.hirelog.api.job.application.summary.view.JobSummaryView
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.presentation.controller.dto.JobSummaryTextReq
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/job-summary")
class JobSummaryController(
    private val jobSummaryQuery: JobSummaryQuery,
    private val jobSummaryFacadeService: JobSummaryGenerationFacadeService,
    private val jdPreprocessRequestService: JdPreprocessRequestService,

    ) {

    /**
     * JobSummary 최신 1건 조회
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

        // 1️⃣ 조회 조건 구성 (유스케이스 모델)
        val condition = JobSummarySearchCondition(
            brandId = brandId,
            positionId = positionId,
            keyword = null
        )

        // 2️⃣ 최신 1건 조회용 Pageable
        val pageable = PageRequest.of(
            0,
            1   // 최신 1건만 필요
        )

        // 3️⃣ 조회 실행
        val result = jobSummaryQuery
            .search(condition, pageable)
            .content
            .firstOrNull()
            ?: throw IllegalArgumentException("JobSummary not found")

        return ResponseEntity.ok(result)
    }

    /**
     * JD 텍스트 요약 요청
     *
     * 처리 방식:
     * - 비동기 파이프라인 진입
     * - 즉시 200 반환
     *
     * 주의:
     * - LLM 벤더는 내부 구현
     */
    @PostMapping("/text")
    fun requestSummary(
        @Valid @RequestBody request: JobSummaryTextReq
    ): ResponseEntity<Void> {

        jdPreprocessRequestService.requestSummary(
            brandName = request.brandName,
            positionName = request.positionName,
            rawText = request.jdText,
            source = JobSourceType.TEXT
        )

        return ResponseEntity.ok().build()
    }
}
