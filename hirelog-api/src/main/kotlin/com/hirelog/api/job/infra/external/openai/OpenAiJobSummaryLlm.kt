package com.hirelog.api.job.infra.external.openai

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
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
    private val circuitBreaker: CircuitBreaker
) : JobSummaryLlm {

    override fun summarizeJobDescriptionAsync(
        brandName: String,
        positionName: String,
        positionCandidates: List<String>,
        existCompanies: List<String>,
        canonicalMap: Map<String, List<String>>
    ): CompletableFuture<JobSummaryLlmResult> {

        val jdText = CanonicalTextNormalizer.toCanonicalText(canonicalMap)

        val prompt = GeminiPromptBuilder.buildJobSummaryPrompt(
            brandName = brandName,
            positionName = positionName,
            positionCandidates = positionCandidates,
            existCompanies = existCompanies,
            jdText = jdText
        )

        log.info(
            "[OPENAI_LLM_ENTRY] brand={}, position={}, circuitBreakerState={}",
            brandName, positionName, circuitBreaker.state
        )

        val decoratedSupplier = circuitBreaker.decorateCompletionStage {
            openAiClient.generateContentAsync(prompt)
        }

        return decoratedSupplier.get()
            .toCompletableFuture()
            .handle { rawText, callEx ->
                if (callEx != null) {
                    val cause = unwrap(callEx)
                    log.error(
                        "[OPENAI_LLM_FAILED] brand={}, position={}, exceptionType={}, message={}",
                        brandName, positionName, cause.javaClass.simpleName, cause.message
                    )
                    throw GeminiCallException(cause)
                }

                log.info("[OPENAI_LLM_SUCCESS] brand={}, position={}", brandName, positionName)

                try {
                    val rawResult = responseParser.parseRawJobSummary(rawText)
                    assembler.assemble(raw = rawResult, provider = LlmProvider.OPENAI)
                } catch (ex: Exception) {
                    log.error(
                        "[OPENAI_PARSE_FAILED] brand={}, position={}, message={}",
                        brandName, positionName, ex.message
                    )
                    throw GeminiParseException(ex)
                }
            }
    }

    private fun unwrap(ex: Throwable): Throwable =
        if (ex is CompletionException) ex.cause ?: ex else ex
}
