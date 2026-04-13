package com.hirelog.api.job.application.rag.model

enum class RagIntent {
    /** 자연어 질문과 유사한 공고 찾기 (k-NN) */
    DOCUMENT_SEARCH,

    /** 저장/지원/합격 공고의 특징 패턴 분석 (aggregation) */
    PATTERN_ANALYSIS,

    /** 사용자 면접/전형 경험 기반 분석 (DB HiringStageRecord) */
    EXPERIENCE_ANALYSIS,

    /** 저장한 공고 통계 (OpenSearch aggregation) */
    STATISTICS,

    /** 기존 BM25 키워드 검색 fallback */
    KEYWORD_SEARCH,

    /** 공고 내용 요약/정리 (k-NN) */
    SUMMARY
}
