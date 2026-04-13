package com.hirelog.api.job.presentation.controller

import com.hirelog.api.job.application.rag.port.RagLlmParser
import com.hirelog.api.job.presentation.controller.dto.response.RagParseRes
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * RAG Admin 컨트롤러
 *
 * RAG 파이프라인 단계별 QA / 디버깅용 엔드포인트.
 * Phase 3~4 구현 전 Parser 단독 테스트 목적.
 */
@RestController
@RequestMapping("/api/admin/rag")
@PreAuthorize("hasRole('ADMIN')")
class RagAdminController(
    private val ragLlmParser: RagLlmParser
) {

    data class ParseReq(
        @field:NotBlank
        @field:Size(max = 500)
        val question: String
    )

    /**
     * LLM Parser 단독 테스트
     *
     * 자연어 질문 → GeminiRagParserAdapter → RagQuery 결과 반환.
     * intent 분류, filters 추출, semanticRetrieval 판단이 올바른지 확인용.
     */
    @PostMapping("/parse")
    fun parse(
        @Valid @RequestBody req: ParseReq
    ): ResponseEntity<RagParseRes> {
        val query = ragLlmParser.parse(req.question)
        return ResponseEntity.ok(RagParseRes.from(query))
    }
}