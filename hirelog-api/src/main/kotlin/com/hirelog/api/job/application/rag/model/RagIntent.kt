package com.hirelog.api.job.application.rag.model

enum class RagIntent {
    /** 자연어 질문과 유사한 공고 찾기 (k-NN + BM25 Hybrid) */
    DOCUMENT_SEARCH,

    /** 공고 내용 요약/정리 (k-NN + BM25 Hybrid) */
    SUMMARY,

    /** 저장한 공고 통계 / cohort 패턴 분석 (OpenSearch aggregation) */
    STATISTICS,

    /** 사용자 면접/전형 경험 기반 분석 (DB HiringStageRecord) */
    EXPERIENCE_ANALYSIS,
}
