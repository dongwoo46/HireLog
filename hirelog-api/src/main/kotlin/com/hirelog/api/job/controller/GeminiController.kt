package com.hirelog.api.job.controller

import com.hirelog.api.job.dto.GeminiSummaryTextRequest
import com.hirelog.api.job.dto.JobSummaryResult
import com.hirelog.api.job.service.GeminiService
import com.hirelog.api.job.service.JobSummaryFacadeService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/gemini")
class GeminiController(
    private val jobSummaryFacadeService: JobSummaryFacadeService
) {

    /**
     * JD 분석 테스트용 API
     *
     * ⚠️ 내부 테스트 전용
     * ⚠️ 인증/인가 붙이기 전까지 외부 노출 금지
     */
    @PostMapping("/summary/text")
    fun summarizeJobDescription(
        @Valid @RequestBody request: GeminiSummaryTextRequest
    ): ResponseEntity<JobSummaryResult> {

        val result = jobSummaryFacadeService.summarizeTextJDAndSave(
            brandName = request.brandName,
            positionName = request.positionName,
            rawText = request.jdText
        )

        return ResponseEntity.ok(result)
    }
}
