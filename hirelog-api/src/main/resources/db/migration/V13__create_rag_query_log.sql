-- RAG 질의 로그 테이블
-- 전체 파이프라인 재현 가능성을 위해 모든 단계 결과를 보존
-- 향후 답변 품질 분석, 프롬프트 개선, 사용 패턴 분석에 활용

CREATE TABLE rag_query_log (
    id              BIGSERIAL       PRIMARY KEY,
    member_id       BIGINT          NOT NULL,

    -- [Step 1] 사용자 질문 원본
    question        TEXT            NOT NULL,

    -- [Step 2] LLM Parser 추출 결과
    intent          VARCHAR(50)     NOT NULL,
    parsed_text     TEXT,
    parsed_filters_json TEXT,

    -- [Step 3] RAG 실행 중간 결과 (검색 문서 / 집계 / 경험 기록)
    -- RagContext JSON: { documents, aggregations, textFeatures, stageRecords }
    context_json    TEXT,

    -- [Step 4] LLM Composer 최종 결과
    answer          TEXT            NOT NULL,
    reasoning       TEXT,
    evidences_json  TEXT,
    sources_json    TEXT,

    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_rag_query_log_member_id   ON rag_query_log (member_id);
CREATE INDEX idx_rag_query_log_intent      ON rag_query_log (intent);
CREATE INDEX idx_rag_query_log_created_at  ON rag_query_log (created_at DESC);
