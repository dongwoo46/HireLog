package com.hirelog.api.job.application.rag.port

import com.hirelog.api.job.application.rag.model.RagAnswer
import com.hirelog.api.job.application.rag.model.RagIntent

/**
 * RAG LLM Composer 포트
 *
 * 책임:
 * - 검색/집계 결과 컨텍스트 + 원본 질문 → 자연어 답변 + 근거(reasoning/evidences) 생성
 */
interface RagLlmComposer {

    /**
     * @param question   원본 유저 질문
     * @param intent     실행된 intent (프롬프트 스타일 결정에 사용)
     * @param context    Executor가 수집한 컨텍스트 (검색 결과, aggregation 수치, 경험 기록 등)
     */
    fun compose(
        question: String,
        intent: RagIntent,
        context: RagContext
    ): RagAnswer
}

/**
 * Executor → Composer 전달 컨텍스트
 *
 * documents:    k-NN 검색 결과 또는 ids filter 조회 문서 목록
 * aggregations: techStack/companyDomain 등 집계 결과 (PATTERN_ANALYSIS, STATISTICS)
 * stageRecords: HiringStageRecord 원문 (EXPERIENCE_ANALYSIS)
 * baselineAggregations: 전체 분포 (PATTERN_ANALYSIS baseline=true일 때)
 */
data class RagContext(
    val documents: List<RagDocument> = emptyList(),
    val aggregations: Map<String, Long> = emptyMap(),
    val baselineAggregations: Map<String, Long> = emptyMap(),
    val stageRecords: List<RagStageRecord> = emptyList()
)

data class RagDocument(
    val id: Long,
    val brandName: String,
    val positionName: String,
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,
    val techStackParsed: List<String>?,
    val idealCandidate: String?,
    val mustHaveSignals: String?,
    val technicalContext: String?,
    val score: Float? = null
)

data class RagStageRecord(
    val brandName: String,
    val positionName: String,
    val stage: String,
    val note: String,
    val result: String?
)