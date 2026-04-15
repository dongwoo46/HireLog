package com.hirelog.api.job.presentation.controller

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.job.application.rag.RagService
import com.hirelog.api.job.presentation.controller.dto.response.RagAnswerRes
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/rag")
class RagController(
    private val ragService: RagService
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
}
