package com.hirelog.api.job.controller

import com.hirelog.api.job.dto.GeminiSummaryResponse
import com.hirelog.api.job.dto.GeminiSummaryRequest
import com.hirelog.api.job.dto.JobSummaryResult
import com.hirelog.api.job.service.GeminiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/gemini")
class GeminiController(
    private val geminiService: GeminiService
) {

    /**
     * JD 분석 테스트용 API
     *
     * ⚠️ 내부 테스트 전용
     * ⚠️ 인증/인가 붙이기 전까지 외부 노출 금지
     */
    @PostMapping("/summary")
    fun analyzeJobDescription(
        @RequestBody request: GeminiSummaryRequest
    ): ResponseEntity<JobSummaryResult> {

        if (request.jdText.isBlank()) {
            return ResponseEntity.badRequest().build()
        }

        val result = geminiService.summaryJobDescription(request.jdText)

        return ResponseEntity.ok(result)
    }
}
