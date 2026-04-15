package com.hirelog.api.job.application.rag.port

/**
 * RAG 쿼리 임베딩 포트
 *
 * JD 인덱싱용 JobSummaryEmbedding과 별도 포트.
 * Python 서버의 POST /embed/query 호출 (단일 text → 벡터).
 */
interface RagEmbedding {
    /**
     * @param text LLM이 파싱한 유저 스킬/조건 텍스트
     * @return 768차원 벡터
     */
    fun embedQuery(text: String): List<Float>
}