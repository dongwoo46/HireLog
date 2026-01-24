package com.hirelog.api.job.infrastructure.external.gemini

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.infra.external.gemini.JobSummaryLlmResultAssembler
import com.hirelog.api.job.intake.similarity.CanonicalTextNormalizer

/**
 * Gemini 기반 JD 요약 LLM Adapter
 *
 * 책임:
 * - JobSummaryLlm Port 구현체
 * - Gemini API 호출 및 응답 처리
 *
 * 설계 원칙:
 * - 외부 LLM 응답은 신뢰하지 않는다
 * - raw 응답 파싱과 도메인 모델 조립을 분리한다
 * - LLM Provider 정보는 시스템에서 주입한다
 */
class GeminiJobSummaryLlm(
    private val geminiClient: GeminiClient,
    private val responseParser: GeminiResponseParser,
    private val assembler: JobSummaryLlmResultAssembler
) : JobSummaryLlm {

    /**
     * Job Description을 Gemini LLM으로 요약한다.
     *
     * 처리 흐름:
     * 1. JD 요약 프롬프트 생성
     * 2. Gemini API 호출 → raw 텍스트 획득
     * 3. raw 텍스트를 JobSummaryLlmResult로 파싱
     *
     * 주의:
     * - 외부 API 호출이 포함되므로 트랜잭션 외부에서 실행되어야 한다
     */
    override fun summarizeJobDescription(
        brandName: String,
        positionName: String,
        positionCandidates: List<String>,
        canonicalMap: Map<String, List<String>>
    ): JobSummaryLlmResult {

        // 1️⃣ canonicalMap → LLM 입력용 텍스트 변환
        val jdText = CanonicalTextNormalizer.toCanonicalText(canonicalMap)

        // 2️⃣ JD 요약 프롬프트 생성
        val prompt = GeminiPromptBuilder.buildJobSummaryPrompt(
            brandName = brandName,
            positionName = positionName,
            positionCandidates = positionCandidates,
            jdText = jdText
        )

        // 2️⃣ Gemini API 호출 → raw 텍스트
        val rawText = try {
            geminiClient.generateContent(prompt)
        } catch (ex: Exception) {
            log.error(
                "Gemini API call failed. brand={}, position={}",
                brandName,
                positionName,
                ex
            )
            throw GeminiCallException(ex)
        }

        // 3️⃣ raw 텍스트 → RawResult
        val rawResult = try {
            responseParser.parseRawJobSummary(rawText)
        } catch (ex: Exception) {
            log.error(
                "Gemini raw parsing failed. brand={}, position={}",
                brandName,
                positionName,
                ex
            )
            throw GeminiParseException(ex)
        }

        // 4️⃣ RawResult → 시스템 책임 모델 조립
        return assembler.assemble(
            raw = rawResult,
            provider = LlmProvider.GEMINI
        )
    }
}
