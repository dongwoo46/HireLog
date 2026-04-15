package com.hirelog.api.job.presentation.controller

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.job.application.rag.RagLogReadService
import com.hirelog.api.job.application.rag.RagService
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.presentation.controller.dto.response.RagAnswerRes
import com.hirelog.api.job.presentation.controller.dto.response.RagQueryLogRes
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/rag")
class RagController(
    private val ragService: RagService,
    private val ragLogReadService: RagLogReadService
) {

    data class RagQueryReq(
        @field:NotBlank
        @field:Size(max = 500)
        val question: String
    )

    /**
     * RAG 질의응답
     *
     * - USER: 하루 최대 3회 (RagRateLimiter)
     * - ADMIN: 제한 없음
     */
    @PostMapping("/query")
    fun query(
        @Valid @RequestBody req: RagQueryReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<RagAnswerRes> {
        val answer = ragService.query(
            question = req.question,
            memberId = member.memberId,
            isAdmin = member.isAdmin()
        )
        return ResponseEntity.ok(RagAnswerRes.from(answer))
    }

    /**
     * 내 RAG 질의 기록 조회
     *
     * 필터:
     * - intent: 특정 intent만 (null → 전체)
     * - dateFrom / dateTo: 날짜 범위 (inclusive, yyyy-MM-dd)
     */
    @GetMapping("/logs")
    fun myLogs(
        @RequestParam(required = false) intent: RagIntent?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<PagedResult<RagQueryLogRes>> {
        val result = ragLogReadService.searchMine(
            memberId = member.memberId,
            intent = intent,
            dateFrom = dateFrom,
            dateTo = dateTo,
            page = page,
            size = size
        )
        return ResponseEntity.ok(result.map { RagQueryLogRes.from(it) })
    }
}
