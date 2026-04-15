package com.hirelog.api.job.application.summary.port

/**
 * JobSummary 임베딩 포트
 *
 * 책임:
 * - JobSummary 핵심 필드 → 768차원 벡터 변환
 * - 모델: jhgan/ko-sroberta-multitask
 */
interface JobSummaryEmbedding {

    fun embed(request: EmbedRequest): List<Float>

    data class EmbedRequest(
        val responsibilities: String,
        val requiredQualifications: String,
        val preferredQualifications: String?,
        val idealCandidate: String?,
        val mustHaveSignals: String?,
        val technicalContext: String?
    )
}