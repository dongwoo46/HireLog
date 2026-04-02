package com.hirelog.api.job.application.summary

class SnapshotAlreadySummarizedException(
    snapshotId: Long,
    existingSummaryId: Long? = null
) : RuntimeException(
    if (existingSummaryId != null) {
        "JobSummary already exists for snapshotId=$snapshotId, summaryId=$existingSummaryId"
    } else {
        "JobSummary already exists for snapshotId=$snapshotId"
    }
)
