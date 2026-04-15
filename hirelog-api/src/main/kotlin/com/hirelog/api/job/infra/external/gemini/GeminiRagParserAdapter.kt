package com.hirelog.api.job.infra.external.gemini

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.rag.model.RagFilters
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.model.RagQuery
import com.hirelog.api.job.application.rag.port.RagLlmParser
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.job.infrastructure.external.gemini.GeminiClient
import com.hirelog.api.job.infrastructure.external.gemini.GeminiPromptBuilder
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * Gemini 기반 RAG LLM Parser 어댑터
 *
 * 책임:
 * - 유저 자연어 질문 → RagQuery (intent, filters, semanticRetrieval 등)
 * - Gemini API 동기 호출 (Spring MVC 컨텍스트)
 * - JSON 파싱 실패 시 KEYWORD_SEARCH fallback (예외 전파 금지)
 */
@Component
class GeminiRagParserAdapter(
    private val geminiClient: GeminiClient,
    private val objectMapper: ObjectMapper
) : RagLlmParser {

    override fun parse(question: String): RagQuery {
        return runCatching {
            val prompt = GeminiPromptBuilder.buildRagParserPrompt(question)
            val rawText = geminiClient.generateContentWithSystemAsync(
                systemInstruction = GeminiPromptBuilder.buildRagParserSystemInstruction(),
                prompt = prompt
            ).get()

            val normalized = rawText
                .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
                .replace("```", "")
                .trim()

            val raw = objectMapper.readValue<RagParserRawResult>(normalized)
            raw.toRagQuery(question)

        }.onFailure { e ->
            log.error(
                "[RAG_PARSER_FAILED] question={}, errorClass={}, error={}",
                question, e.javaClass.simpleName, e.message
            )
        }.getOrElse {
            keywordSearchFallback(question)
        }
    }

    private fun keywordSearchFallback(question: String) = RagQuery(
        intent = RagIntent.KEYWORD_SEARCH,
        semanticRetrieval = false,
        aggregation = false,
        baseline = false,
        filters = RagFilters(),
        parsedText = question
    )

    // ─────────────────────────────────────────────────────────────
    // 내부 Raw 파싱 모델 (Gemini JSON 응답 매핑)
    // ─────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class RagParserRawResult(
        val intent: String?,
        val semanticRetrieval: Boolean?,
        val aggregation: Boolean?,
        val baseline: Boolean?,
        val parsedText: String?,
        val filters: RagFiltersRaw?
    ) {
        fun toRagQuery(originalQuestion: String): RagQuery {
            val intent = runCatching { RagIntent.valueOf(intent ?: "") }
                .getOrElse { RagIntent.KEYWORD_SEARCH }

            return RagQuery(
                intent = intent,
                semanticRetrieval = semanticRetrieval ?: (intent == RagIntent.DOCUMENT_SEARCH || intent == RagIntent.SUMMARY),
                aggregation = aggregation ?: false,
                baseline = baseline ?: false,
                parsedText = parsedText?.takeIf { it.isNotBlank() } ?: originalQuestion,
                filters = filters?.toRagFilters() ?: RagFilters()
            )
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class RagFiltersRaw(
        val saveType: String?,
        val stage: String?,
        val stageResult: String?,
        val careerType: String?,
        val companyDomain: String?,
        val techStacks: List<String>?,
        val brandName: String?,
        val dateRangeFrom: String?,
        val dateRangeTo: String?
    ) {
        fun toRagFilters() = RagFilters(
            saveType = runCatching { saveType?.let { MemberJobSummarySaveType.valueOf(it) } }.getOrNull(),
            stage = runCatching { stage?.let { HiringStage.valueOf(it) } }.getOrNull(),
            stageResult = runCatching { stageResult?.let { HiringStageResult.valueOf(it) } }.getOrNull(),
            careerType = careerType,
            companyDomain = companyDomain,
            techStacks = techStacks,
            brandName = brandName,
            dateRangeFrom = dateRangeFrom,
            dateRangeTo = dateRangeTo
        )
    }
}
