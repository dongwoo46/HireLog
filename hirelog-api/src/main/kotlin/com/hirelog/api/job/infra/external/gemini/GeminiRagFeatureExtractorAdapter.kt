package com.hirelog.api.job.infra.external.gemini

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.rag.port.RagLlmFeatureExtractor
import com.hirelog.api.job.infrastructure.external.gemini.GeminiClient
import com.hirelog.api.job.infrastructure.external.gemini.GeminiPromptBuilder
import org.springframework.stereotype.Component

/**
 * Gemini 기반 RAG Feature Extractor 어댑터
 *
 * 책임:
 * - cohort 문서 전처리 텍스트 → 공통 특징 레이블 목록 추출
 * - 파싱 실패 시 빈 리스트 반환 (STATISTICS 흐름 중단 방지)
 */
@Component
class GeminiRagFeatureExtractorAdapter(
    private val geminiClient: GeminiClient,
    private val objectMapper: ObjectMapper
) : RagLlmFeatureExtractor {

    override fun extractFeatureLabels(preprocessedTexts: List<String>): List<String> {
        return runCatching {
            val rawText = geminiClient.generateContentWithSystemAsync(
                systemInstruction = GeminiPromptBuilder.buildFeatureExtractorSystemInstruction(),
                prompt = GeminiPromptBuilder.buildFeatureExtractorPrompt(preprocessedTexts)
            ).get()

            val normalized = rawText
                .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
                .replace("```", "")
                .trim()

            objectMapper.readValue<List<String>>(normalized)
                .filter { it.isNotBlank() }

        }.onFailure { e ->
            log.error(
                "[RAG_FEATURE_EXTRACTOR_FAILED] docCount={}, errorClass={}, error={}",
                preprocessedTexts.size, e.javaClass.simpleName, e.message
            )
        }.getOrElse { emptyList() }
    }
}
