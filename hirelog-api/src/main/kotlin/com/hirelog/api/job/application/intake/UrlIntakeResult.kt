package com.hirelog.api.job.application.intake

import com.hirelog.api.job.application.summary.view.JobSummaryView

sealed class UrlIntakeResult {
    data class Duplicate(val existing: JobSummaryView) : UrlIntakeResult()
    data class NewRequest(val requestId: String) : UrlIntakeResult()
}
