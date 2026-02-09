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
import org.springframework.stereotype.Component

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
            "[GEMINI_LLM_ENTRY] brand={}, position={}, circuitBreakerState={}",
            brandName, positionName, circuitBreaker.state
        )

        // CircuitBreaker로 감싼 supplier
        val decoratedSupplier = circuitBreaker.decorateCompletionStage {
            geminiClient.generateContentAsync(prompt)
        }

        return decoratedSupplier.get()
            .toCompletableFuture()
            .handle { rawText, callEx ->
                if (callEx != null) {
                    val cause = unwrap(callEx)
                    log.error(
                        "[GEMINI_LLM_FAILED] brand={}, position={}, exceptionType={}, message={}",
                        brandName, positionName, cause.javaClass.simpleName, cause.message
                    )
                    throw GeminiCallException(cause)
                }

                log.info("[GEMINI_LLM_SUCCESS] brand={}, position={}", brandName, positionName)

                try {
                    val rawResult = responseParser.parseRawJobSummary(rawText)
                    assembler.assemble(raw = rawResult, provider = LlmProvider.GEMINI)
                } catch (ex: Exception) {
                    log.error(
                        "[GEMINI_PARSE_FAILED] brand={}, position={}, message={}",
                        brandName, positionName, ex.message
                    )
                    throw GeminiParseException(ex)
                }
            }
    }

    private fun unwrap(ex: Throwable): Throwable =
        if (ex is CompletionException) ex.cause ?: ex else ex
}