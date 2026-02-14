package com.hirelog.api.job.infrastructure.external.gemini

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.infra.external.common.LlmResponseParser
import com.hirelog.api.job.infra.external.common.JobSummaryLlmResultAssembler
import com.hirelog.api.job.intake.similarity.CanonicalTextNormalizer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.Counter
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
    private val responseParser: LlmResponseParser,
    private val assembler: JobSummaryLlmResultAssembler,
    private val circuitBreaker: CircuitBreaker,
    meterRegistry: MeterRegistry
) : JobSummaryLlm {

    private val latencyTimer: Timer = Timer.builder("llm.call.latency")
        .tag("provider", "gemini")
        .publishPercentileHistogram()
        .register(meterRegistry)

    private val successCounter: Counter = Counter.builder("llm.call.success")
        .tag("provider", "gemini")
        .register(meterRegistry)

    private val failCounter: Counter = Counter.builder("llm.call.fail")
        .tag("provider", "gemini")
        .register(meterRegistry)

    override fun summarizeJobDescriptionAsync(
        brandName: String,
        positionName: String,
        positionCandidates: List<String>,
        existCompanies: List<String>,
        canonicalMap: Map<String, List<String>>
    ): CompletableFuture<JobSummaryLlmResult> {

        val sample = Timer.start()

        val jdText = CanonicalTextNormalizer.toCanonicalText(canonicalMap)

        val prompt = GeminiPromptBuilder.buildJobSummaryPrompt(
            brandName,
            positionName,
            positionCandidates,
            existCompanies,
            jdText
        )

        val decoratedSupplier = circuitBreaker.decorateCompletionStage {
            geminiClient.generateContentAsync(prompt)
        }

        return decoratedSupplier.get()
            .toCompletableFuture()
            .handle { rawText, callEx ->

                if (callEx != null) {
                    failCounter.increment()
                    sample.stop(latencyTimer)
                    throw GeminiCallException(unwrap(callEx))
                }

                try {
                    val rawResult = responseParser.parseRawJobSummary(rawText)
                    val result = assembler.assemble(
                        raw = rawResult,
                        provider = LlmProvider.GEMINI
                    )

                    successCounter.increment()
                    sample.stop(latencyTimer)
                    result

                } catch (ex: Exception) {
                    failCounter.increment()
                    sample.stop(latencyTimer)
                    throw GeminiParseException(ex)
                }
            }
    }

    private fun unwrap(ex: Throwable): Throwable =
        if (ex is CompletionException) ex.cause ?: ex else ex
}
