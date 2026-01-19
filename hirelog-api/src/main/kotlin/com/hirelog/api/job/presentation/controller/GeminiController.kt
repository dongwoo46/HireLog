package com.hirelog.api.job.presentation.controller

import com.hirelog.api.job.application.summary.facade.JobSummaryFacadeService
import com.hirelog.api.job.presentation.controller.dto.GeminiSummaryTextReq
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/gemini")
class GeminiController(
    private val jobSummaryFacadeService: JobSummaryFacadeService
) {

    /**
     * JD 텍스트 요약 요청 (Gemini)
     *
     * 용도:
     * - 내부 테스트
     * - LLM 파이프라인 검증
     *
     * 주의:
     * - 인증/인가 전까지 외부 노출 금지
     * - 동기 처리 (추후 비동기 전환 가능)
     */
    @PostMapping("/summary/text")
    fun summarizeJobDescription(
        @Valid @RequestBody request: GeminiSummaryTextReq
    ): ResponseEntity<Void> {

        jobSummaryFacadeService.summarizeTextAndSave(
            brandName = request.brandName,
            positionName = request.positionName,
            rawText = request.jdText
        )

        return ResponseEntity.ok().build()
    }


}
