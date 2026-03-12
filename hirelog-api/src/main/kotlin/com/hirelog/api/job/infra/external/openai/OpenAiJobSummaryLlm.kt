package com.hirelog.api.job.infra.external.openai

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.Counter
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.infra.external.common.LlmResponseParser
import com.hirelog.api.job.infra.external.common.JobSummaryLlmResultAssembler
import com.hirelog.api.job.intake.similarity.CanonicalTextNormalizer
import com.hirelog.api.job.infrastructure.external.gemini.GeminiPromptBuilder
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * OpenAI 기반 JD 요약 LLM Adapter
 *
 * 책임:
 * - JobSummaryLlm Port 구현체 (OpenAI)
 * - Gemini fallback용
 *
 * 설계 원칙:
 * - 동일 프롬프트 (GeminiPromptBuilder 재사용)
 * - 동일 응답 파싱 (GeminiResponseParser 재사용)
 * - 동일 결과 조립 (JobSummaryLlmResultAssembler 재사용)
 */

class OpenAiJobSummaryLlm(
    private val openAiClient: OpenAiClient,
    private val responseParser: LlmResponseParser,
    private val assembler: JobSummaryLlmResultAssembler,
    private val circuitBreaker: CircuitBreaker,
    meterRegistry: MeterRegistry
) : JobSummaryLlm {

    private val latencyTimer: Timer = Timer.builder("llm.call.latency")
        .tag("provider", "openai")
        .publishPercentileHistogram()
        .register(meterRegistry)

    private val successCounter: Counter = Counter.builder("llm.call.success")
        .tag("provider", "openai")
        .register(meterRegistry)

    private val failCounter: Counter = Counter.builder("llm.call.fail")
        .tag("provider", "openai")
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
            brandName = brandName,
            positionName = positionName,
            positionCandidates = positionCandidates,
            existCompanies = existCompanies,
            jdText = jdText
        )

        val decoratedSupplier = circuitBreaker.decorateCompletionStage {
            openAiClient.generateContentAsync(prompt)
        }

        return decoratedSupplier.get()
            .toCompletableFuture()
            .handle { rawText, callEx ->

                if (callEx != null) {
                    val cause = unwrap(callEx)
                    failCounter.increment()
                    sample.stop(latencyTimer)
                    throw GeminiCallException(cause)
                }

                try {
                    val rawResult = responseParser.parseRawJobSummary(rawText)
                    val result = assembler.assemble(
                        raw = rawResult,
                        provider = LlmProvider.OPENAI
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
