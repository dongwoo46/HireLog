package com.hirelog.api.job.presentation.controller.dto.response

import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.view.RagQueryLogView
import java.time.LocalDateTime

/**
 * RAG 질의 로그 응답 DTO
 *
 * JSON 필드(parsedFiltersJson, contextJson, evidencesJson, sourcesJson)는
 * 문자열 그대로 반환 — 클라이언트가 파싱하거나 분석 도구에서 직접 활용
 */
data class RagQueryLogRes(
    val id: Long,
    val memberId: Long,
    val question: String,
    val intent: RagIntent,
    val parsedText: String?,

    /** LLM Parser 필터 추출 결과 (JSON 문자열) */
    val parsedFiltersJson: String?,

    /** RAG 실행 중간 결과 — 문서/집계/경험기록 (JSON 문자열) */
    val contextJson: String?,

    val answer: String,
    val reasoning: String?,

    /** 답변 근거 목록 (JSON 문자열) */
    val evidencesJson: String?,

    /** 출처 공고 목록 (JSON 문자열) */
    val sourcesJson: String?,

    val createdAt: LocalDateTime
) {
    companion object {
        fun from(view: RagQueryLogView) = RagQueryLogRes(
            id = view.id,
            memberId = view.memberId,
            question = view.question,
            intent = view.intent,
            parsedText = view.parsedText,
            parsedFiltersJson = view.parsedFiltersJson,
            contextJson = view.contextJson,
            answer = view.answer,
            reasoning = view.reasoning,
            evidencesJson = view.evidencesJson,
            sourcesJson = view.sourcesJson,
            createdAt = view.createdAt
        )
    }
}
