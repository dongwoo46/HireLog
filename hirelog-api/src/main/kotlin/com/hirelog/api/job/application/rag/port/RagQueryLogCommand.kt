package com.hirelog.api.job.application.rag.port

import com.hirelog.api.job.domain.model.RagQueryLog

/**
 * RAG 질의 로그 저장 포트
 */
interface RagQueryLogCommand {
    fun save(log: RagQueryLog)
}
