package com.hirelog.api.job.infra.external.embedding.dto

data class EmbedHttpRequest(
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,
    val idealCandidate: String?,
    val mustHaveSignals: String?,
    val technicalContext: String?
)