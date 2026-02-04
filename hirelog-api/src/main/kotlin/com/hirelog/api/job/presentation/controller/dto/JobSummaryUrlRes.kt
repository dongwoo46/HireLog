package com.hirelog.api.job.presentation.controller.dto

import com.hirelog.api.job.application.summary.view.JobSummaryView

/**
 * URL 기반 JD 요약 요청 응답
 *
 * 정책:
 * - 신규 요청: requestId 반환, duplicate = false
 * - 중복 요청: existingSummary 반환, duplicate = true
 */
data class JobSummaryUrlRes(
    val duplicate: Boolean,
    val requestId: String? = null,
    val existingSummary: JobSummaryView? = null
) {
    companion object {
        fun newRequest(requestId: String) = JobSummaryUrlRes(
            duplicate = false,
            requestId = requestId
        )

        fun duplicateOf(summary: JobSummaryView) = JobSummaryUrlRes(
            duplicate = true,
            existingSummary = summary
        )
    }
}
