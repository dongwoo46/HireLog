package com.hirelog.api.job.presentation.controller

import com.hirelog.api.job.application.summary.JobSummaryAdminService
import com.hirelog.api.job.infrastructure.external.gemini.GeminiPromptBuilder
import com.hirelog.api.job.presentation.controller.dto.GeminiPromptPreviewReq
import com.hirelog.api.job.presentation.controller.dto.GeminiPromptRes
import com.hirelog.api.job.presentation.controller.dto.JobSummaryAdminCreateReq
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Admin 전용 JobSummary 관리 컨트롤러
 *
 * 기능:
 * - Python 전처리 파이프라인 없이 직접 Gemini 호출하여 JobSummary 생성
 * - 수동 데이터 처리용
 */
@RestController
@RequestMapping("/api/admin/job-summary")
@PreAuthorize("hasRole('ADMIN')")
class JobSummaryAdminController(
    private val jobSummaryAdminService: JobSummaryAdminService
) {

    /**
     * Admin 전용 JobSummary 직접 생성
     *
     * 처리 방식:
     * - Python 전처리 파이프라인 스킵
     * - JD 원문 텍스트로 직접 Gemini 호출
     * - 동기 처리 (응답까지 대기)
     *
     * @return 생성된 JobSummary ID
     */
    @PostMapping("/direct")
    fun createDirectly(
        @Valid @RequestBody request: JobSummaryAdminCreateReq
    ): ResponseEntity<Map<String, Long>> {

        val summaryId = jobSummaryAdminService.createDirectly(
            brandName = request.brandName,
            positionName = request.positionName,
            jdText = request.jdText,
            sourceUrl = request.sourceUrl
        )

        return ResponseEntity.ok(mapOf("summaryId" to summaryId))
    }

    /**
     * Gemini System Instruction 조회
     *
     * 현재 설정된 system instruction 확인용
     */
    @GetMapping("/prompt/system-instruction")
    fun getSystemInstruction(): ResponseEntity<GeminiPromptRes> {
        return ResponseEntity.ok(
            GeminiPromptRes(
                systemInstruction = GeminiPromptBuilder.buildSystemInstruction()
            )
        )
    }

    /**
     * Gemini 프롬프트 미리보기
     *
     * JD 데이터를 넣으면 실제 Gemini에 전송될 프롬프트 확인
     */
    @PostMapping("/prompt/preview")
    fun previewPrompt(
        @Valid @RequestBody request: GeminiPromptPreviewReq
    ): ResponseEntity<GeminiPromptRes> {

        val userPrompt = GeminiPromptBuilder.buildJobSummaryPrompt(
            brandName = request.brandName,
            positionName = request.positionName,
            positionCandidates = request.positionCandidates,
            existCompanies = request.existCompanies,
            jdText = request.jdText
        )

        return ResponseEntity.ok(
            GeminiPromptRes(
                systemInstruction = GeminiPromptBuilder.buildSystemInstruction(),
                userPrompt = userPrompt
            )
        )
    }
}
