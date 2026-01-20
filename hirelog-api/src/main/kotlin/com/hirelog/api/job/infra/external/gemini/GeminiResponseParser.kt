package com.hirelog.api.job.infrastructure.external.gemini

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult

/**
 * Gemini 응답 파서
 *
 * 책임:
 * - Markdown / CodeBlock 제거
 * - JSON 파싱
 */
class GeminiResponseParser(
    private val objectMapper: ObjectMapper
) {

    fun parseJobSummary(rawText: String): JobSummaryLlmResult {
        val normalized = rawText
            .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace("```", "")
            .trim()

        try {
            return objectMapper.readValue(
                normalized,
                JobSummaryLlmResult::class.java
            )
        } catch (e: JsonProcessingException) {
            throw GeminiParseException(e)
        }
    }
}
