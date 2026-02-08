package com.hirelog.api.job.application.intake.model

/**
 * 중복 판정 결과
 *
 * sealed class로 변경하여 중복 시 관련 정보 포함
 */
sealed class DuplicateDecision {

    object NotDuplicate : DuplicateDecision()

    /**
     * 중복 판정됨
     *
     * @param reason 중복 사유 (HASH / SIMHASH / TRGM)
     * @param existingSnapshotId 기존 Snapshot ID
     * @param existingSummaryId 기존 JobSummary ID (Summary가 있는 경우)
     */
    data class Duplicate(
        val reason: DuplicateReason,
        val existingSnapshotId: Long,
        val existingSummaryId: Long?
    ) : DuplicateDecision()

    val isDuplicate: Boolean
        get() = this is Duplicate
}

enum class DuplicateReason {
    HASH,
    SIMHASH,
    TRGM,
    URL
}
