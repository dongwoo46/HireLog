package com.hirelog.api.job.infra.external.gemini

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.rag.model.RagAnswer
import com.hirelog.api.job.application.rag.model.RagEvidence
import com.hirelog.api.job.application.rag.model.RagEvidenceType
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.model.RagSource
import com.hirelog.api.job.application.rag.port.RagContext
import com.hirelog.api.job.application.rag.port.RagLlmComposer
import com.hirelog.api.job.infrastructure.external.gemini.GeminiClient
import com.hirelog.api.job.infrastructure.external.gemini.GeminiPromptBuilder
import org.springframework.stereotype.Component

/**
 * Gemini 기반 RAG Composer 어댑터
 *
 * 책임:
 * - RagContext(구조화 결과) + 원본 질문 → 자연어 응답(RagAnswer)
 * - intent별 context를 텍스트로 직렬화 후 Gemini에 전달
 * - 파싱 실패 시 에러 메시지 RagAnswer 반환 (예외 전파 금지)
 */
@Component
class GeminiRagComposerAdapter(
    private val geminiClient: GeminiClient,
    private val objectMapper: ObjectMapper
) : RagLlmComposer {

    companion object {
        private const val AGGREGATION_TOP_N = 10
        private val FALLBACK_ANSWER = RagAnswer(
            answer = "답변을 생성하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            intent = RagIntent.DOCUMENT_SEARCH,
            reasoning = null,
            evidences = null,
            sources = null
        )
    }

    override fun compose(question: String, intent: RagIntent, context: RagContext): RagAnswer {
        return runCatching {
            val contextText = buildContextText(context)
            val rawText = geminiClient.generateContentWithSystemAsync(
                systemInstruction = GeminiPromptBuilder.buildComposerSystemInstruction(),
                prompt = GeminiPromptBuilder.buildComposerPrompt(question, intent.name, contextText)
            ).get()

            val normalized = rawText
                .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
                .replace("```", "")
                .trim()

            objectMapper.readValue<ComposerRawResult>(normalized).toRagAnswer(intent)

        }.onFailure { e ->
            log.error(
                "[RAG_COMPOSER_FAILED] intent={}, errorClass={}, error={}",
                intent, e.javaClass.simpleName, e.message
            )
        }.getOrElse { FALLBACK_ANSWER.copy(intent = intent) }
    }

    // ─────────────────────────────────────────────────────────────
    // Context → 텍스트 직렬화
    // ─────────────────────────────────────────────────────────────

    private fun buildContextText(context: RagContext): String = buildString {
        if (context.documents.isNotEmpty()) {
            appendLine("## 검색된 공고 (${context.documents.size}건)")
            context.documents.forEachIndexed { idx, doc ->
                appendLine("[${idx + 1}] id=${doc.id} | ${doc.brandName} | ${doc.positionName}")
                doc.companyDomain?.let { appendLine("  도메인: $it") }
                doc.companySize?.let { appendLine("  규모: $it") }
                doc.techStackParsed?.takeIf { it.isNotEmpty() }
                    ?.let { appendLine("  기술스택: ${it.joinToString(", ")}") }
                doc.responsibilities.takeIf { it.isNotBlank() }
                    ?.let { appendLine("  주요 업무: $it") }
                doc.requiredQualifications.takeIf { it.isNotBlank() }
                    ?.let { appendLine("  자격 요건: $it") }
            }
            appendLine()
        }

        if (context.aggregations.isNotEmpty()) {
            appendLine("## 집계 결과")
            context.aggregations.groupBy { it.category }.forEach { (category, entries) ->
                appendLine("### $category")
                entries.take(AGGREGATION_TOP_N).forEach { entry ->
                    val multiplierText = entry.baselineMultiplier
                        ?.let { " (전체 대비 %.1fx)".format(it) } ?: ""
                    appendLine("  ${entry.label}: ${entry.cohortCount}건$multiplierText")
                }
            }
            appendLine()
        }

        if (context.textFeatures.isNotEmpty()) {
            appendLine("## 정성적 특징")
            context.textFeatures.forEach { feature ->
                appendLine("- ${feature.feature} (${feature.observedCount}건 관찰)")
                feature.snippets.forEach { snippet -> appendLine("  > $snippet") }
            }
            appendLine()
        }

        if (context.stageRecords.isNotEmpty()) {
            appendLine("## 전형 경험 기록 (${context.stageRecords.size}건)")
            appendLine("(아래 각 항목의 '기록 내용'이 분석의 핵심 재료입니다)")
            context.stageRecords.forEach { record ->
                appendLine("")
                appendLine("### ${record.brandName} — ${record.positionName}")
                appendLine("전형 단계: ${record.stage} | 결과: ${record.result ?: "미기록"}")
                record.note.takeIf { it.isNotBlank() }?.let {
                    appendLine("기록 내용:")
                    appendLine(it)
                }
            }
            appendLine()
        }

        if (context.reviewRecords.isNotEmpty()) {
            appendLine("## 공고 리뷰 (${context.reviewRecords.size}건)")
            context.reviewRecords.forEach { review ->
                appendLine("")
                appendLine("### ${review.brandName} — ${review.positionName}")
                appendLine("전형 단계: ${review.hiringStage} | 난이도: ${review.difficultyRating}/10 | 만족도: ${review.satisfactionRating}/10")
                appendLine("장점: ${review.prosComment}")
                appendLine("단점: ${review.consComment}")
                review.tip?.takeIf { it.isNotBlank() }?.let { appendLine("팁: $it") }
            }
        }
    }.trim()

    // ─────────────────────────────────────────────────────────────
    // 내부 Raw 파싱 모델
    // ─────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class ComposerRawResult(
        val answer: String?,
        val reasoning: String?,
        val evidences: List<EvidenceRaw>?,
        val sources: List<SourceRaw>?
    ) {
        fun toRagAnswer(intent: RagIntent) = RagAnswer(
            answer = answer?.takeIf { it.isNotBlank() } ?: "답변을 생성할 수 없습니다.",
            intent = intent,
            reasoning = reasoning?.takeIf { it.isNotBlank() },
            evidences = evidences?.mapNotNull { it.toRagEvidence() }?.takeIf { it.isNotEmpty() },
            sources = sources?.map { RagSource(id = it.id, brandName = it.brandName, positionName = it.positionName) }
                ?.takeIf { it.isNotEmpty() }
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EvidenceRaw(
        val type: String?,
        val summary: String?,
        val detail: String?,
        val sourceId: Long?
    ) {
        fun toRagEvidence(): RagEvidence? {
            val evidenceType = runCatching { RagEvidenceType.valueOf(type ?: "") }.getOrNull()
                ?: return null
            val summaryText = summary?.takeIf { it.isNotBlank() } ?: return null
            return RagEvidence(
                type = evidenceType,
                summary = summaryText,
                detail = detail?.takeIf { it.isNotBlank() },
                sourceId = sourceId
            )
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class SourceRaw(
        val id: Long,
        val brandName: String,
        val positionName: String
    )
}