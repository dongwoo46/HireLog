package com.hirelog.api.job.infrastructure.external.gemini

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.infra.external.gemini.JobSummaryLlmResultAssembler
import com.hirelog.api.job.intake.similarity.CanonicalTextNormalizer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * Gemini 기반 JD 요약 LLM Adapter
 *
 * 책임:
 * - JobSummaryLlm Port 구현체
 * - Gemini API 비동기 호출 및 응답 파싱
 *
 * 설계 원칙:
 * - 외부 LLM 응답은 신뢰하지 않는다
 * - raw 응답 파싱과 도메인 모델 조립을 분리한다
 * - 호출 스레드를 차단하지 않는다 (NIO 스레드에서 HTTP 처리)
 * - 예외는 GeminiCallException / GeminiParseException으로 래핑하여 전파
 */
class GeminiJobSummaryLlm(
    private val geminiClient: GeminiClient,
    private val responseParser: GeminiResponseParser,
    private val assembler: JobSummaryLlmResultAssembler
) : JobSummaryLlm {

    override fun summarizeJobDescriptionAsync(
        brandName: String,
        positionName: String,
        positionCandidates: List<String>,
        canonicalMap: Map<String, List<String>>
    ): CompletableFuture<JobSummaryLlmResult> {

        val jdText = CanonicalTextNormalizer.toCanonicalText(canonicalMap)

        val prompt = GeminiPromptBuilder.buildJobSummaryPrompt(
            brandName = brandName,
            positionName = positionName,
            positionCandidates = positionCandidates,
            jdText = jdText
        )

        return geminiClient.generateContentAsync(prompt)
            .handle { rawText, callEx ->
                if (callEx != null) {
                    log.error(
                        "Gemini API call failed. brand={}, position={}",
                        brandName, positionName, unwrap(callEx)
                    )
                    throw GeminiCallException(unwrap(callEx))
                }

                try {
                    val rawResult = responseParser.parseRawJobSummary(rawText)
                    assembler.assemble(raw = rawResult, provider = LlmProvider.GEMINI)
                } catch (ex: Exception) {
                    log.error(
                        "Gemini response parsing failed. brand={}, position={}",
                        brandName, positionName, ex
                    )
                    throw GeminiParseException(ex)
                }
            }
    }

    private fun unwrap(ex: Throwable): Throwable =
        if (ex is CompletionException) ex.cause ?: ex else ex
}
