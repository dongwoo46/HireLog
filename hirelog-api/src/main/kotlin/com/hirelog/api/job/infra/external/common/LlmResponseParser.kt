package com.hirelog.api.job.infra.external.common

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.job.application.summary.view.JobSummaryLlmRawResult

/**
 * LLM 응답 파서 (공통)
 *
 * 책임:
 * - LLM이 반환한 raw 텍스트를 JSON으로 파싱
 * - Markdown / CodeBlock 제거
 *
 * 설계 원칙:
 * - LLM 종류(Gemini, OpenAI)와 무관하게 동일 로직 적용
 * - 이 단계에서는 "정합성"을 보장하지 않는다
 * - 누락된 필드, null 값 허용
 * - 도메인 판단은 절대 하지 않는다
 */
class LlmResponseParser(
    private val objectMapper: ObjectMapper
) {

    /**
     * LLM 응답을 Raw Result로 파싱
     *
     * 역할:
     * - LLM 응답을 그대로 구조화
     * - 이후 Assembler 단계에서 정규화 / 보정 수행
     *
     * @param rawText LLM이 반환한 원본 텍스트
     */
    fun parseRawJobSummary(rawText: String): JobSummaryLlmRawResult {

        // 1️⃣ Markdown / CodeBlock 제거
        val normalized = rawText
            .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace("```", "")
            .trim()

        // 2️⃣ JSON 파싱
        try {
            return objectMapper.readValue(
                normalized,
                JobSummaryLlmRawResult::class.java
            )
        } catch (e: JsonProcessingException) {
            throw GeminiParseException(e)
        }
    }
}
