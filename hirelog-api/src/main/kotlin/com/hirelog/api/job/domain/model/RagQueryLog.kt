package com.hirelog.api.job.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.job.application.rag.model.RagIntent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * RAG 질의 로그
 *
 * 파이프라인 전 단계를 저장하여 답변 재현 가능성을 보장한다.
 *
 * [Step 1] question          — 사용자 질문 원본
 * [Step 2] intent / parsedText / parsedFiltersJson — LLM Parser 추출 결과
 * [Step 3] contextJson       — RAG 실행 중간 결과 (문서 / 집계 / 경험 기록)
 * [Step 4] answer / reasoning / evidencesJson / sourcesJson — LLM Composer 결과
 *
 * 설계:
 * - 불변 엔티티 (저장 후 수정 없음, updatable = false)
 * - 복잡한 객체는 JSON TEXT 직렬화 (RagFilters, RagContext, RagEvidence, RagSource)
 */
@Entity
@Table(name = "rag_query_log")
class RagQueryLog protected constructor(

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    // ─── Step 1: 사용자 입력 ───────────────────────────────────────

    @Column(name = "question", nullable = false, updatable = false, columnDefinition = "TEXT")
    val question: String,

    // ─── Step 2: LLM Parser 결과 ──────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false, updatable = false, length = 50)
    val intent: RagIntent,

    /** Parser가 검색용으로 추출한 키워드 */
    @Column(name = "parsed_text", updatable = false, columnDefinition = "TEXT")
    val parsedText: String?,

    /** Parser가 추출한 필터 조건 (RagFilters JSON) */
    @Column(name = "parsed_filters_json", updatable = false, columnDefinition = "TEXT")
    val parsedFiltersJson: String?,

    // ─── Step 3: RAG 실행 중간 결과 ──────────────────────────────

    /**
     * Executor가 수집한 컨텍스트 (RagContext JSON)
     *
     * documents    : 검색된 공고 목록 (DOCUMENT_SEARCH / SUMMARY)
     * aggregations : 기술스택/도메인/규모 집계 (STATISTICS)
     * textFeatures : LLM Feature Extractor 추출 정성 특징 (STATISTICS + cohort)
     * stageRecords : 사용자 전형 경험 기록 (EXPERIENCE_ANALYSIS)
     */
    @Column(name = "context_json", updatable = false, columnDefinition = "TEXT")
    val contextJson: String?,

    // ─── Step 4: LLM Composer 결과 ───────────────────────────────

    @Column(name = "answer", nullable = false, updatable = false, columnDefinition = "TEXT")
    val answer: String,

    @Column(name = "reasoning", updatable = false, columnDefinition = "TEXT")
    val reasoning: String?,

    /** 답변 근거 목록 (List<RagEvidence> JSON) */
    @Column(name = "evidences_json", updatable = false, columnDefinition = "TEXT")
    val evidencesJson: String?,

    /** 출처 공고 목록 (List<RagSource> JSON) */
    @Column(name = "sources_json", updatable = false, columnDefinition = "TEXT")
    val sourcesJson: String?

) : BaseEntity() {

    companion object {
        fun create(
            memberId: Long,
            question: String,
            intent: RagIntent,
            parsedText: String?,
            parsedFiltersJson: String?,
            contextJson: String?,
            answer: String,
            reasoning: String?,
            evidencesJson: String?,
            sourcesJson: String?
        ) = RagQueryLog(
            memberId = memberId,
            question = question,
            intent = intent,
            parsedText = parsedText,
            parsedFiltersJson = parsedFiltersJson,
            contextJson = contextJson,
            answer = answer,
            reasoning = reasoning,
            evidencesJson = evidencesJson,
            sourcesJson = sourcesJson
        )
    }
}
