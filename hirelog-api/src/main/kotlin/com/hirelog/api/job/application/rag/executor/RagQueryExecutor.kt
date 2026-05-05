package com.hirelog.api.job.application.rag.executor

import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.model.RagQuery
import com.hirelog.api.job.application.rag.port.AggregationEntry
import com.hirelog.api.job.application.rag.port.RagCohortQuery
import com.hirelog.api.job.application.rag.port.RagContext
import com.hirelog.api.job.application.rag.port.RagDocument
import com.hirelog.api.job.application.rag.port.RagEmbedding
import com.hirelog.api.job.application.rag.port.RagLlmFeatureExtractor
import com.hirelog.api.job.application.rag.port.TextFeature
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter.AggregationResult
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter.RawDocumentFields
import org.springframework.stereotype.Component

/**
 * RAG Query Executor
 *
 * 책임:
 * - RagQuery intent별 실행 분기
 * - DB / OpenSearch / LLM Feature Extractor 조합으로 RagContext 구성
 * - Composer(LLM)에 넘길 모든 계산/가공을 완료
 *
 * 설계:
 * - Composer는 구조화된 결과만 받으며 raw 수치를 직접 처리하지 않음
 * - STATISTICS cohort 분석 시 LLM Feature Extractor 추가 호출 (총 3회)
 * - 그 외 intent는 LLM 2회 (Parser + Composer)
 */
@Component
class RagQueryExecutor(
    private val ragEmbedding: RagEmbedding,
    private val ragCohortQuery: RagCohortQuery,
    private val ragLlmFeatureExtractor: RagLlmFeatureExtractor,
    private val openSearchAdapter: JobSummaryOpenSearchAdapter
) {

    companion object {
        private const val HYBRID_TOP_N = 10
        private const val HYBRID_CANDIDATE_SIZE = 20
        private const val AGGREGATION_BUCKET_SIZE = 30
        private const val RRF_K = 60

        // 전처리 필드별 최대 길이
        private const val MAX_RESPONSIBILITIES = 200
        private const val MAX_REQUIRED_QUALIFICATIONS = 200
        private const val MAX_PREFERRED_QUALIFICATIONS = 150
        private const val MAX_TECH_STACK = 100
        private const val MAX_DOC_TOTAL = 600

        // 스니펫 추출 시 앞뒤 여백
        private const val SNIPPET_CONTEXT_CHARS = 40
    }

    fun execute(ragQuery: RagQuery, memberId: Long): RagContext = when (ragQuery.intent) {
        RagIntent.DOCUMENT_SEARCH, RagIntent.SUMMARY -> executeHybridSearch(ragQuery)
        RagIntent.STATISTICS -> executeStatistics(ragQuery, memberId)
        RagIntent.EXPERIENCE_ANALYSIS -> executeExperienceAnalysis(ragQuery, memberId)
    }

    // ─────────────────────────────────────────────────────────────
    // DOCUMENT_SEARCH / SUMMARY
    // ─────────────────────────────────────────────────────────────

    private fun executeHybridSearch(ragQuery: RagQuery): RagContext {
        val vector = ragEmbedding.embedQuery(ragQuery.parsedText)
        val results = openSearchAdapter.searchHybrid(
            queryVector = vector,
            keyword = ragQuery.parsedText,
            topN = HYBRID_TOP_N,
            candidateSize = HYBRID_CANDIDATE_SIZE,
            careerType = ragQuery.filters.careerType,
            companyDomain = ragQuery.filters.companyDomain
        )
        return RagContext(documents = results.map { it.toRagDocument() })
    }

    // ─────────────────────────────────────────────────────────────
    // STATISTICS
    // ─────────────────────────────────────────────────────────────

    private fun executeStatistics(ragQuery: RagQuery, memberId: Long): RagContext {
        val cohortIds = resolveCohortIds(ragQuery, memberId)

        // cohort 조건 있는데 결과 없으면 빈 컨텍스트
        if (cohortIds != null && cohortIds.isEmpty()) return RagContext()

        // aggregation
        val cohortAgg = openSearchAdapter.aggregateFields(
            ids = cohortIds,
            careerType = ragQuery.filters.careerType,
            companyDomain = ragQuery.filters.companyDomain,
            size = AGGREGATION_BUCKET_SIZE
        )
        val baselineAgg = if (ragQuery.baseline && cohortIds != null) {
            openSearchAdapter.aggregateFields(ids = null, size = AGGREGATION_BUCKET_SIZE)
        } else null

        val aggregations = buildAggregationEntries(cohortAgg, baselineAgg, ragQuery.focusTechStack)

        // cohort 텍스트 특징 추출 (cohort 있을 때만)
        val textFeatures = if (cohortIds != null && cohortIds.isNotEmpty()) {
            val rawDocs = openSearchAdapter.findCohortDocumentTexts(cohortIds)
            val preprocessedTexts = rawDocs.map { it.preprocess() }
            val featureLabels = ragLlmFeatureExtractor.extractFeatureLabels(preprocessedTexts)
            buildTextFeatures(featureLabels, rawDocs)
        } else emptyList()

        return RagContext(aggregations = aggregations, textFeatures = textFeatures)
    }

    // ─────────────────────────────────────────────────────────────
    // EXPERIENCE_ANALYSIS
    // ─────────────────────────────────────────────────────────────

    private fun executeExperienceAnalysis(ragQuery: RagQuery, memberId: Long): RagContext {
        val stageRecords = ragCohortQuery.findStageRecordsForRag(
            memberId = memberId,
            stage = ragQuery.filters.stage,
            stageResult = ragQuery.filters.stageResult
        )
        val reviewRecords = ragCohortQuery.findReviewsByMemberId(memberId)
        return RagContext(stageRecords = stageRecords, reviewRecords = reviewRecords)
    }

    // ─────────────────────────────────────────────────────────────
    // 내부 계산 로직
    // ─────────────────────────────────────────────────────────────

    private fun resolveCohortIds(ragQuery: RagQuery, memberId: Long): List<Long>? {
        val hasCohortFilter = ragQuery.filters.saveType != null
            || ragQuery.filters.stage != null
            || ragQuery.filters.stageResult != null
        if (!hasCohortFilter) return null

        return ragCohortQuery.findJobSummaryIdsByCohort(
            memberId = memberId,
            saveType = ragQuery.filters.saveType,
            stage = ragQuery.filters.stage,
            stageResult = ragQuery.filters.stageResult
        )
    }

    private fun buildAggregationEntries(
        cohortAgg: AggregationResult,
        baselineAgg: AggregationResult?,
        focusTechStack: Boolean
    ): List<AggregationEntry> {
        fun buildEntries(
            cohortMap: Map<String, Long>,
            baselineMap: Map<String, Long>?,
            category: String
        ): List<AggregationEntry> {
            val cohortTotal = cohortMap.values.sum().coerceAtLeast(1L)
            val baselineTotal = baselineMap?.values?.sum()?.coerceAtLeast(1L)

            return cohortMap.map { (label, count) ->
                val baselineCount = baselineMap?.get(label) ?: 0L
                val multiplier = if (baselineAgg != null && baselineTotal != null) {
                    if (baselineCount == 0L) null
                    else (count.toDouble() / cohortTotal) / (baselineCount.toDouble() / baselineTotal)
                } else null
                AggregationEntry(category = category, label = label, cohortCount = count, baselineMultiplier = multiplier)
            }.sortedByDescending { it.cohortCount }
        }

        return buildList {
            if (focusTechStack) addAll(buildEntries(cohortAgg.techStacks, baselineAgg?.techStacks, "techStack"))
            addAll(buildEntries(cohortAgg.careerTypes, baselineAgg?.careerTypes, "careerType"))
            addAll(buildEntries(cohortAgg.positionCategories, baselineAgg?.positionCategories, "positionCategory"))
            addAll(buildEntries(cohortAgg.companyDomains, baselineAgg?.companyDomains, "companyDomain"))
            addAll(buildEntries(cohortAgg.companySizes, baselineAgg?.companySizes, "companySize"))
        }
    }

    private fun buildTextFeatures(
        featureLabels: List<String>,
        rawDocs: List<RawDocumentFields>
    ): List<TextFeature> {
        return featureLabels.mapNotNull { label ->
            val matchingDocs = rawDocs.filter { it.containsFeature(label) }
            if (matchingDocs.isEmpty()) return@mapNotNull null

            TextFeature(
                feature = label,
                observedCount = matchingDocs.size,
                snippets = matchingDocs.take(3).mapNotNull { it.extractSnippet(label) },
                sourceIds = matchingDocs.map { it.id }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 전처리 / 변환
    // ─────────────────────────────────────────────────────────────

    /**
     * 문서 원본 필드 전처리
     *
     * 1. 필드별 normalize (연속 공백/줄바꿈 → 단일 공백) + truncate
     * 2. 필드명 포함 concat
     * 3. 문서 전체 soft limit(MAX_DOC_TOTAL) 초과 시 truncate
     */
    private fun RawDocumentFields.preprocess(): String {
        val parts = buildList {
            responsibilities?.normalizeAndTruncate(MAX_RESPONSIBILITIES)
                ?.let { add("responsibilities: $it") }
            requiredQualifications?.normalizeAndTruncate(MAX_REQUIRED_QUALIFICATIONS)
                ?.let { add("requiredQualifications: $it") }
            preferredQualifications?.normalizeAndTruncate(MAX_PREFERRED_QUALIFICATIONS)
                ?.let { add("preferredQualifications: $it") }
            techStackParsed?.joinToString(", ")?.normalizeAndTruncate(MAX_TECH_STACK)
                ?.let { add("techStack: $it") }
        }

        val joined = parts.joinToString(" ")
        return if (joined.length > MAX_DOC_TOTAL) joined.substring(0, MAX_DOC_TOTAL) else joined
    }

    private fun String.normalizeAndTruncate(maxLength: Int): String? {
        val normalized = this.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return null
        return if (normalized.length > maxLength) normalized.substring(0, maxLength) else normalized
    }

    private fun RawDocumentFields.containsFeature(label: String): Boolean {
        val allText = listOfNotNull(
            responsibilities,
            requiredQualifications,
            preferredQualifications,
            techStackParsed?.joinToString(", ")
        ).joinToString(" ").lowercase()
        return allText.contains(label.lowercase())
    }

    private fun RawDocumentFields.extractSnippet(label: String): String? {
        val allText = listOfNotNull(responsibilities, requiredQualifications, preferredQualifications)
            .joinToString(" ")
        val idx = allText.lowercase().indexOf(label.lowercase())
        if (idx == -1) return null
        val start = maxOf(0, idx - SNIPPET_CONTEXT_CHARS)
        val end = minOf(allText.length, idx + label.length + SNIPPET_CONTEXT_CHARS)
        return allText.substring(start, end).replace(Regex("\\s+"), " ").trim()
    }

    private fun JobSummaryOpenSearchAdapter.KnnSearchResult.toRagDocument() = RagDocument(
        id = id,
        brandName = brandName,
        positionName = positionName,
        companyDomain = companyDomain,
        companySize = companySize,
        responsibilities = responsibilities,
        requiredQualifications = requiredQualifications,
        preferredQualifications = preferredQualifications,
        techStackParsed = techStackParsed,
        idealCandidate = idealCandidate,
        mustHaveSignals = mustHaveSignals,
        technicalContext = technicalContext,
        score = score
    )
}
