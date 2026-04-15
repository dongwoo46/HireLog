# Embedding Server

## 개요

`src/embedding/` — JobSummary 벡터 임베딩 FastAPI 서버

RAG 파이프라인의 임베딩 담당 컴포넌트.
- JobSummary 인덱싱 시 6개 필드 → 768차원 벡터 생성
- RAG 쿼리 시 유저 텍스트 → 벡터 생성 (k-NN 검색용)

---

## 파일 구조

```
src/embedding/
├── __init__.py
├── config.py    ← 모델명/host/port 환경변수 관리
├── model.py     ← 싱글톤 모델 로더 + 디바이스 자동 선택 + 워밍업
├── router.py    ← /embed, /embed/query 엔드포인트
└── main.py      ← FastAPI app + uvicorn 진입점
```

---

## 모델

| 항목 | 값 |
|---|---|
| 모델 | `jhgan/ko-sroberta-multitask` |
| 차원 | 768 |
| 환경변수 | `EMBEDDING_MODEL` (기본값: `jhgan/ko-sroberta-multitask`) |

- 문서 저장 모델 = 쿼리 임베딩 모델 (반드시 동일)
- 모델 변경 시 OpenSearch 전체 재인덱싱 필요

---

## 엔드포인트

### POST /embed — JobSummary 인덱싱용

**Request**
```json
{
  "responsibilities": "string (필수)",
  "requiredQualifications": "string (필수)",
  "preferredQualifications": "string | null",
  "idealCandidate": "string | null",
  "mustHaveSignals": "string | null",
  "technicalContext": "string | null"
}
```

**Response**
```json
{
  "vector": [0.12, -0.03, ...],
  "dim": 768,
  "model": "jhgan/ko-sroberta-multitask"
}
```

6개 필드를 `\n` 구분으로 concat 후 임베딩. concat 로직은 Python에서 관리.

---

### POST /embed/query — RAG k-NN 검색용

**Request**
```json
{
  "text": "LLM이 파싱한 유저 스킬/조건 텍스트"
}
```

**Response**
```json
{
  "vector": [0.12, -0.03, ...],
  "dim": 768,
  "model": "jhgan/ko-sroberta-multitask"
}
```

유저 자연어 질문을 LLM이 파싱한 결과를 단순 text로 전달.

---

## 실행

```bash
cd src
python -m embedding.main

# 환경변수 지정 시
EMBEDDING_MODEL=jhgan/ko-sroberta-multitask \
EMBEDDING_SERVER_HOST=0.0.0.0 \
EMBEDDING_SERVER_PORT=8000 \
python -m embedding.main
```

---

## 호출 흐름

### 인덱싱 흐름
```
JobSummaryIndexingConsumer (Spring)
  → POST /embed { 6개 필드 }
  → 768차원 벡터
  → JobSummarySearchPayload.embeddingVector
  → OpenSearch knn_vector 저장
```

### RAG 검색 흐름
```
유저 자연어 질문
  → LLM Parser (Spring)
  → POST /embed/query { text }
  → 768차원 벡터
  → OpenSearch k-NN 검색
  → LLM Answer Composer
```

---

## 주의사항

- `dim != 768` 응답 시 Spring에서 예외 처리 필요
- 디바이스 자동 선택: CUDA → MPS → CPU 순
- 서버 시작 시 워밍업 1회 실행 (첫 요청 레이턴시 제거)