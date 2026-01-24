package com.hirelog.api.job.application.summary.port

import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult

/**
 * JD 요약용 LLM 포트
 *
 * 책임:
 * - Job Description을 구조화된 요약 결과로 변환
 *
 * 설계 의도:
 * - LLM 종류(Gemini, OpenAI, Claude)와 무관하게 사용
 */
interface JobSummaryLlm {

    fun summarizeJobDescription(
        brandName: String,
        positionName: String,
        positionCandidates: List<String>,
        canonicalMap: Map<String, List<String>>
    ): JobSummaryLlmResult
}
