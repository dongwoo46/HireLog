package com.hirelog.api.job.presentation.controller.dto.response

import com.hirelog.api.job.application.rag.model.RagFilters
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.model.RagQuery

/**
 * LLM Parser 결과 응답 DTO
 *
 * Admin QA / 디버깅 전용.
 * GeminiRagParserAdapter가 질문을 어떻게 해석했는지 확인.
 */
data class RagParseRes(
    val intent: RagIntent,
    val semanticRetrieval: Boolean,
    val aggregation: Boolean,
    val baseline: Boolean,
    val parsedText: String,
    val filters: RagFilters
) {
    companion object {
        fun from(query: RagQuery) = RagParseRes(
            intent = query.intent,
            semanticRetrieval = query.semanticRetrieval,
            aggregation = query.aggregation,
            baseline = query.baseline,
            parsedText = query.parsedText,
            filters = query.filters
        )
    }
}