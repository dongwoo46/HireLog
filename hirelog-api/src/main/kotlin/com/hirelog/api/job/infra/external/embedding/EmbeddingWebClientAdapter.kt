package com.hirelog.api.job.infra.external.embedding

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryEmbedding
import com.hirelog.api.job.infra.external.embedding.dto.EmbedHttpRequest
import com.hirelog.api.job.infra.external.embedding.dto.EmbedHttpResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * JobSummary 임베딩 WebClient 어댑터
 *
 * 책임:
 * - Python FastAPI 임베딩 서버 호출
 * - POST /embed → 768차원 벡터 반환
 *
 * 실패 정책:
 * - dim 불일치 시 예외 (모델 불일치 방어)
 * - 호출 실패 시 예외 전파 → Consumer에서 재시도
 */
@Component
class EmbeddingWebClientAdapter(
    @Qualifier("embeddingWebClient") private val webClient: WebClient
) : JobSummaryEmbedding {

    override fun embed(request: JobSummaryEmbedding.EmbedRequest): List<Float> {
        val response = webClient.post()
            .uri("/embed")
            .bodyValue(request.toHttpRequest())
            .retrieve()
            .bodyToMono(EmbedHttpResponse::class.java)
            .block()
            ?: throw IllegalStateException("Embedding server returned null response")

        if (response.dim != EXPECTED_DIM) {
            throw IllegalStateException(
                "Unexpected embedding dimension: expected=$EXPECTED_DIM, actual=${response.dim}, model=${response.model}"
            )
        }

        log.debug("[EMBEDDING_SUCCESS] dim={}, model={}", response.dim, response.model)
        return response.vector
    }

    private fun JobSummaryEmbedding.EmbedRequest.toHttpRequest() = EmbedHttpRequest(
        responsibilities = responsibilities,
        requiredQualifications = requiredQualifications,
        preferredQualifications = preferredQualifications,
        idealCandidate = idealCandidate,
        mustHaveSignals = mustHaveSignals,
        technicalContext = technicalContext
    )

    companion object {
        private const val EXPECTED_DIM = 768
    }
}