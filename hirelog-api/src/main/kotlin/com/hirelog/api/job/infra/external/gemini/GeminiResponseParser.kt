package com.hirelog.api.job.infrastructure.external.gemini

import JobSummaryLlmRawResult
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.exception.GeminiParseException

/**
 * Gemini 응답 파서
 *
 * 책임:
 * - Gemini가 반환한 raw 텍스트를 JSON으로 파싱
 * - Markdown / CodeBlock 제거
 *
 * 설계 원칙:
 * - 이 단계에서는 "정합성"을 보장하지 않는다
 * - 누락된 필드, null 값 허용
 * - 도메인 판단은 절대 하지 않는다
 */
class GeminiResponseParser(
    private val objectMapper: ObjectMapper
) {

    /**
     * Gemini 응답을 Raw Result로 파싱
     *
     * 역할:
     * - LLM 응답을 그대로 구조화
     * - 이후 Assembler 단계에서 정규화 / 보정 수행
     *
     * @param rawText Gemini가 반환한 원본 텍스트
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
            // 🔥 이 단계에서 실패하면 "LLM 응답 자체가 깨진 것"
            throw GeminiParseException(e)
        }
    }
}
