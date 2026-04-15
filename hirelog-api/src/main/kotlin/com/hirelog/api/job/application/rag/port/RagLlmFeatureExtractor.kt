package com.hirelog.api.job.application.rag.port

/**
 * RAG 텍스트 특징 추출 포트
 *
 * 책임:
 * - cohort 문서 전처리 텍스트 → 공통 특징 레이블 목록 추출
 *
 * 설계:
 * - LLM은 레이블만 추출 (예: ["대용량 트래픽", "MSA", "데이터 파이프라인"])
 * - count / snippet / sourceId 계산은 Executor 담당 (hallucination 방지)
 * - STATISTICS intent + cohort 조건 있을 때만 호출
 */
interface RagLlmFeatureExtractor {

    /**
     * @param preprocessedTexts 문서별 전처리 텍스트 목록
     *                          형식: "responsibilities: ... requiredQualifications: ... ..."
     * @return 공통 특징 레이블 목록
     */
    fun extractFeatureLabels(preprocessedTexts: List<String>): List<String>
}