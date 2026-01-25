package com.hirelog.api.job.application.intake.model

data class IntakeHashes(
    val canonicalHash: String,
    val simHash: Long,
    val coreText: String
)
