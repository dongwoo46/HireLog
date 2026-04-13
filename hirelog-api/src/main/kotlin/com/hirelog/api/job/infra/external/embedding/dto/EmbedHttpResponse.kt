package com.hirelog.api.job.infra.external.embedding.dto

data class EmbedHttpResponse(
    val vector: List<Float>,
    val dim: Int,
    val model: String
)