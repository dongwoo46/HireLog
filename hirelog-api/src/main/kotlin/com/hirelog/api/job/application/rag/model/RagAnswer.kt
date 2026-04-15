package com.hirelog.api.job.application.rag.model

/**
 * RAG 최종 응답
 */
data class RagAnswer(
    /** 자연어 답변 */
    val answer: String,

    val intent: RagIntent,

    /**
     * 답변 근거 및 사고 과정
     *
     * Composer가 어떤 정보를 바탕으로, 어떤 논리로 이 답변을 도출했는지.
     * 예시:
     * - "저장한 공고 12건 중 Kafka가 8건(67%)에 등장했고, 전체 평균 23% 대비 2.9배 높아
     *    백엔드 메시징 경험이 핵심 요건으로 판단했습니다."
     * - "질문에서 '핀테크 백엔드'를 감지해 유사 공고 top-10을 검색한 결과,
     *    Spring Boot + Kafka 조합이 9건에서 공통으로 확인됐습니다."
     */
    val reasoning: String?,

    /**
     * 답변을 뒷받침하는 구체적 근거 목록
     * - k-NN 결과: 유사 공고 발췌
     * - aggregation 결과: 수치 근거
     * - stage record 결과: 경험 발췌
     * null: Composer가 근거를 특정하지 못한 경우
     */
    val evidences: List<RagEvidence>?,

    /**
     * k-NN retrieval 결과 sources (DOCUMENT_SEARCH / SUMMARY일 때만)
     * null: 다른 intent
     */
    val sources: List<RagSource>?
)

data class RagSource(
    val id: Long,
    val brandName: String,
    val positionName: String
)

/**
 * 개별 근거 항목
 *
 * type:    근거 유형 (DOCUMENT, AGGREGATION, EXPERIENCE)
 * summary: 근거 한 줄 요약
 * detail:  상세 내용 (발췌 텍스트 또는 수치)
 * sourceId: 근거가 된 JobSummary ID (문서 근거일 때만)
 */
data class RagEvidence(
    val type: RagEvidenceType,
    val summary: String,
    val detail: String?,
    val sourceId: Long?
)

enum class RagEvidenceType {
    /** 특정 JD 문서에서 발췌한 근거 */
    DOCUMENT,

    /** OpenSearch aggregation 수치 근거 */
    AGGREGATION,

    /** 사용자의 HiringStageRecord에서 추출한 경험 근거 */
    EXPERIENCE
}
