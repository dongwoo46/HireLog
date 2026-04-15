package com.hirelog.api.job.application.rag.model

/**
 * LLM Parser 출력 모델
 *
 * GeminiRagParserAdapter가 유저 자연어 질문을 파싱하여 생성.
 */
data class RagQuery(
    val intent: RagIntent,

    /** true이면 Python 임베딩 서버 호출 (k-NN 검색용) */
    val semanticRetrieval: Boolean,

    /** true이면 OpenSearch aggregation 포함 */
    val aggregation: Boolean,

    /**
     * true이면 PATTERN_ANALYSIS에서 baseline(전체 분포) 비교 수행
     * cohort 비율 / 전체 비율 → 배율 계산
     */
    val baseline: Boolean,

    /** 검색/필터 조건 */
    val filters: RagFilters,

    /**
     * true이면 STATISTICS aggregation에 techStack 버킷 포함
     * 질문이 특정 기술명을 명시적으로 언급할 때만 true
     */
    val focusTechStack: Boolean,

    /**
     * k-NN / aggregation에 사용할 파싱된 텍스트
     * KEYWORD_SEARCH일 때: 키워드 그대로
     * DOCUMENT_SEARCH/SUMMARY일 때: 스킬/역할 조건 텍스트
     */
    val parsedText: String
)