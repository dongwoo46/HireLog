package com.hirelog.api.job.presentation.controller

import com.hirelog.api.job.application.summary.JobSummaryAdminService
import com.hirelog.api.job.application.summary.JobSummaryReadService
import com.hirelog.api.job.application.summary.view.JobSummaryAdminView
import com.hirelog.api.job.application.summary.view.JobSummaryDetailView
import com.hirelog.api.job.infrastructure.external.gemini.GeminiPromptBuilder
import com.hirelog.api.job.presentation.controller.dto.request.GeminiPromptPreviewReq
import com.hirelog.api.job.presentation.controller.dto.response.GeminiPromptRes
import com.hirelog.api.job.presentation.controller.dto.request.JobSummaryAdminCreateReq
import com.hirelog.api.job.presentation.controller.dto.request.VerifyAdminReq
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
    private val jobSummaryAdminService: JobSummaryAdminService,
    private val jobSummaryReadService: JobSummaryReadService
) {

    /**
     * Admin 전용 JobSummary 목록 조회
     *
     * - isActive=true  → 활성화된 것만
     * - isActive=false → 비활성화된 것만
     * - isActive 없음  → 전체 조회
     */
    @GetMapping
    fun list(
        @RequestParam isActive: Boolean? = null,
        @RequestParam brandName: String? = null,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<JobSummaryAdminView>> {
        return ResponseEntity.ok(jobSummaryReadService.searchAdmin(isActive, brandName, page, size))
    }

    /**
     * Admin 전용 JobSummary 상세 조회 - 비활성화된 것도 조회 가능
     */
    @GetMapping("/{id}")
    fun getDetail(@PathVariable id: Long): ResponseEntity<JobSummaryDetailView> {
        val detail = jobSummaryReadService.getDetailAdmin(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(detail)
    }


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
     * 전체 재인덱싱
     *
     * 인덱스 매핑 변경 시 사용 (기존 인덱스 삭제 + 재생성 + 전체 재인덱싱)
     */
    @PostMapping("/reindex-all")
    fun reindexAll(
        @RequestParam(defaultValue = "50") batchSize: Int
    ): ResponseEntity<Map<String, Int>> {
        val successCount = jobSummaryAdminService.reindexAll(batchSize)
        return ResponseEntity.ok(mapOf("successCount" to successCount))
    }

    /**
     * 임베딩 벡터 누락 문서 재임베딩
     *
     * 임베딩 서버 장애 등으로 벡터가 null인 문서를 재처리
     */
    @PostMapping("/reindex-embedding")
    fun reindexMissingEmbeddings(
        @RequestParam(defaultValue = "50") batchSize: Int
    ): ResponseEntity<Map<String, Int>> {
        val successCount = jobSummaryAdminService.reindexMissingEmbeddings(batchSize)
        return ResponseEntity.ok(mapOf("successCount" to successCount))
    }

    @PostMapping("/verify")
    fun verifyAdmin(@Valid @RequestBody request: VerifyAdminReq):ResponseEntity<Void> {
        jobSummaryAdminService.verify(request.password)

        // 검증 성공 → 204 No Content
        return ResponseEntity.noContent().build()
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
