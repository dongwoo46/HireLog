package com.hirelog.api.job.application.summary.port

import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import java.util.concurrent.CompletableFuture

/**
 * JD 요약용 LLM 포트
 *
 * 책임:
 * - Job Description을 구조화된 요약 결과로 비동기 변환
 *
 * 설계 의도:
 * - LLM 종류(Gemini, OpenAI, Claude)와 무관하게 사용
 * - 호출 스레드를 차단하지 않고 CompletableFuture로 결과 전달
 */
interface JobSummaryLlm {

    fun summarizeJobDescriptionAsync(
        brandName: String,
        positionName: String,
        positionCandidates: List<String>,
        categoryCandidates: List<String>,
        canonicalMap: Map<String, List<String>>
    ): CompletableFuture<JobSummaryLlmResult>
}

