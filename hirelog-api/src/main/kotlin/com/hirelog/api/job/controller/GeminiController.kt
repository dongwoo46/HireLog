package com.hirelog.api.job.controller

import com.hirelog.api.job.dto.GeminiAnalyzeRequest
import com.hirelog.api.job.dto.GeminiAnalyzeResponse
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
    @PostMapping("/analyze")
    fun analyzeJobDescription(
        @RequestBody request: GeminiAnalyzeRequest
    ): ResponseEntity<GeminiAnalyzeResponse> {

        if (request.jdText.isBlank()) {
            return ResponseEntity.badRequest().build()
        }

        val result = geminiService.analyzeJobDescription(request.jdText)

        return ResponseEntity.ok(
            GeminiAnalyzeResponse(result = result)
        )
    }
}
