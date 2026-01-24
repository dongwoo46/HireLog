package com.hirelog.api.job.application.intake.model

enum class DuplicateDecision {
    NOT_DUPLICATE,
    HASH_DUPLICATE,
    SIMHASH_DUPLICATE,
    TRGM_DUPLICATE
}
