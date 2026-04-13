package com.hirelog.api.job.application.rag.port

import com.hirelog.api.job.application.rag.model.RagQuery

/**
 * RAG LLM Parser 포트
 *
 * 책임:
 * - 유저 자연어 질문 → RagQuery (intent + filters + semanticRetrieval 등) 변환
 * - 파싱 실패 시 KEYWORD_SEARCH fallback 반환 (예외 전파 금지)
 */
interface RagLlmParser {
    fun parse(question: String): RagQuery
}