package com.hirelog.api.job.application.rag

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.rag.executor.RagQueryExecutor
import com.hirelog.api.job.application.rag.model.RagAnswer
import com.hirelog.api.job.application.rag.port.RagLlmComposer
import com.hirelog.api.job.application.rag.port.RagLlmParser
import org.springframework.stereotype.Service

/**
 * RAG 서비스 오케스트레이터
 *
 * 흐름: RateLimiter → Parser → Executor → Composer
 *
 * 책임:
 * - 일일 호출 제한 검증 (USER: 3회, ADMIN: 무제한)
 * - Parser / Executor / Composer 순차 조합
 * - 트랜잭션 불필요 (외부 LLM / OpenSearch 호출)
 */
@Service
class RagService(
    private val ragLlmParser: RagLlmParser,
    private val ragQueryExecutor: RagQueryExecutor,
    private val ragLlmComposer: RagLlmComposer,
    private val ragRateLimiter: RagRateLimiter
) {

    fun query(question: String, memberId: Long, isAdmin: Boolean): RagAnswer {
        ragRateLimiter.checkAndIncrement(memberId, isAdmin)

        log.info("[RAG_QUERY_START] memberId={}, question={}", memberId, question)

        val ragQuery = ragLlmParser.parse(question)
        log.info(
            "[RAG_QUERY_PARSED] memberId={}, intent={}, semanticRetrieval={}, aggregation={}",
            memberId, ragQuery.intent, ragQuery.semanticRetrieval, ragQuery.aggregation
        )

        val context = ragQueryExecutor.execute(ragQuery, memberId)
        val answer = ragLlmComposer.compose(question, ragQuery.intent, context)

        log.info("[RAG_QUERY_DONE] memberId={}, intent={}", memberId, ragQuery.intent)
        return answer
    }
}
