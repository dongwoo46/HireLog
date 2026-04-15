package com.hirelog.api.job.presentation.controller

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.rag.RagLogReadService
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.port.RagLlmParser
import com.hirelog.api.job.presentation.controller.dto.response.RagParseRes
import com.hirelog.api.job.presentation.controller.dto.response.RagQueryLogRes
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * RAG Admin 컨트롤러
 *
 * - LLM Parser 단독 테스트
 * - RAG 질의 로그 조회 (전체 멤버, 필터 가능)
 */
@RestController
@RequestMapping("/api/admin/rag")
@PreAuthorize("hasRole('ADMIN')")
class RagAdminController(
    private val ragLlmParser: RagLlmParser,
    private val ragLogReadService: RagLogReadService
) {

    data class ParseReq(
        @field:NotBlank
        @field:Size(max = 500)
        val question: String
    )

    /**
     * LLM Parser 단독 테스트
     */
    @PostMapping("/parse")
    fun parse(
        @Valid @RequestBody req: ParseReq
    ): ResponseEntity<RagParseRes> {
        val query = ragLlmParser.parse(req.question)
        return ResponseEntity.ok(RagParseRes.from(query))
    }

    /**
     * RAG 질의 로그 목록 조회 (Admin)
     *
     * 필터:
     * - memberId: 특정 사용자 로그만 (null → 전체)
     * - intent: 특정 intent만 (null → 전체)
     * - dateFrom / dateTo: 날짜 범위 (inclusive, yyyy-MM-dd)
     */
    @GetMapping("/logs")
    fun searchLogs(
        @RequestParam(required = false) memberId: Long?,
        @RequestParam(required = false) intent: RagIntent?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<RagQueryLogRes>> {
        val result = ragLogReadService.searchAdmin(memberId, intent, dateFrom, dateTo, page, size)
        return ResponseEntity.ok(result.map { RagQueryLogRes.from(it) })
    }

    /**
     * RAG 질의 로그 단건 조회 (Admin)
     */
    @GetMapping("/logs/{id}")
    fun getLog(@PathVariable id: Long): ResponseEntity<RagQueryLogRes> {
        val view = ragLogReadService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(RagQueryLogRes.from(view))
    }
}