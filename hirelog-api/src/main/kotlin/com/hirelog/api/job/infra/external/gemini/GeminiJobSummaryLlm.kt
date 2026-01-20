package com.hirelog.api.job.infrastructure.external.gemini

import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult

/**
 * Gemini 기반 JD 요약 LLM Adapter
 *
 * 책임:
 * - JobSummaryLlm Port 구현
 * - Gemini API를 이용한 JD 요약 수행
 *
 * 설계 원칙:
 * - Application 계층에는 LLM 구현 세부를 노출하지 않는다
 * - Gemini 관련 로직은 이 클래스 내부로 완전히 캡슐화한다
 * - Prompt 생성 / API 호출 / 응답 파싱 책임을 명확히 분리한다
 */
class GeminiJobSummaryLlm(
    private val geminiClient: GeminiClient,
    private val responseParser: GeminiResponseParser
) : JobSummaryLlm {

    /**
     * Job Description을 Gemini LLM을 통해 요약한다.
     *
     * 처리 흐름:
     * 1. JD 요약 전용 프롬프트 생성
     * 2. Gemini API 호출을 통해 raw 응답 획득
     * 3. 응답을 공통 LLM 결과 모델로 파싱
     *
     * 주의:
     * - 이 메서드는 외부 API 호출을 포함한다
     * - 반드시 트랜잭션 외부에서 호출되어야 한다
     *
     * @param brandName 채용 브랜드명 (프롬프트 컨텍스트)
     * @param position 포지션명 (프롬프트 컨텍스트)
     * @param jdText JD 원문 텍스트
     * @return 구조화된 JD 요약 결과
     */
    override fun summarizeJobDescription(
        brandName: String,
        position: String,
        jdText: String
    ): JobSummaryLlmResult {

        // JD 요약 전용 프롬프트 생성
        val prompt = GeminiPromptBuilder.buildJobSummaryPrompt(
            brandName = brandName,
            position = position,
            jdText = jdText
        )

        // Gemini API 호출을 통해 raw 응답 획득
        // 1️⃣ Gemini API 호출
        val raw = try {
            geminiClient.generateContent(prompt)
        } catch (ex: Exception) {

            // 🔥 로그에만 상세 컨텍스트 기록
            log.error(
                "Gemini API call failed. brand={}, position={}",
                brandName,
                position,
                ex
            )

            throw GeminiCallException(ex)
        }

        // Gemini 응답을 공통 LLM 결과 모델로 파싱
        return try {
            responseParser.parseJobSummary(raw)
        } catch (ex: Exception) {
            // 🔥 파싱 실패도 로그만
            log.error(
                "Gemini response parse failed. brand={}, position={}",
                brandName,
                position,
                ex
            )
            throw GeminiParseException(ex)
        }
    }
}
