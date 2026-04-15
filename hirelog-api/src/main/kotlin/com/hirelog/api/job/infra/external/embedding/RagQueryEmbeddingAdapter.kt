package com.hirelog.api.job.infra.external.embedding

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.rag.port.RagEmbedding
import com.hirelog.api.job.infra.external.embedding.dto.EmbedHttpResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * RAG 쿼리 임베딩 어댑터
 *
 * 책임:
 * - 유저 질문 텍스트 → 768차원 벡터 변환
 * - Python 임베딩 서버 POST /embed/query 호출
 *
 * JobSummary 인덱싱용 EmbeddingWebClientAdapter와 엔드포인트만 다름.
 * JD 전체 필드가 아닌 단일 text → 쿼리 특화 임베딩 사용.
 */
@Component
class RagQueryEmbeddingAdapter(
    @Qualifier("embeddingWebClient") private val webClient: WebClient
) : RagEmbedding {

    companion object {
        private const val EXPECTED_DIM = 768
    }

    override fun embedQuery(text: String): List<Float> {
        val response = webClient.post()
            .uri("/embed/query")
            .bodyValue(mapOf("text" to text))
            .retrieve()
            .bodyToMono(EmbedHttpResponse::class.java)
            .block()
            ?: throw IllegalStateException("Embedding server returned null response for query")

        if (response.dim != EXPECTED_DIM) {
            throw IllegalStateException(
                "Unexpected embedding dimension: expected=$EXPECTED_DIM, actual=${response.dim}"
            )
        }

        log.debug("[RAG_QUERY_EMBEDDING] dim={}, textLength={}", response.dim, text.length)
        return response.vector
    }
}