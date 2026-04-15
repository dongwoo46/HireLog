# JobSummary 임베딩 & 재인덱싱

## 개요

JobSummary를 OpenSearch에 인덱싱할 때 k-NN 검색용 임베딩 벡터를 함께 저장.
임베딩은 Python FastAPI 서버(`preprocess-pipeline/src/embedding/`)에서 생성.

---

## 임베딩 설정

| 항목 | 값 |
|---|---|
| 모델 | `jhgan/ko-sroberta-multitask` |
| 차원 | 768 |
| spaceType | `cosinesimil` (normalize_embeddings=True) |
| 환경변수 | `EMBEDDING_SERVER_URL` (Spring), `EMBEDDING_MODEL` (Python) |

### 임베딩 대상 필드 (6개)

```
responsibilities, requiredQualifications, preferredQualifications
idealCandidate, mustHaveSignals, technicalContext (Insight)
```

---

## 코드 위치

### Spring

| 역할 | 파일 |
|---|---|
| 임베딩 포트 인터페이스 | `job/application/summary/port/JobSummaryEmbedding.kt` |
| WebClient 어댑터 | `job/infra/external/embedding/EmbeddingWebClientAdapter.kt` |
| HTTP DTO | `job/infra/external/embedding/dto/EmbedHttpRequest.kt`, `EmbedHttpResponse.kt` |
| WebClient 빈 | `common/config/webclient/WebClientConfig.kt` (`embeddingWebClient`) |
| 설정 Properties | `common/config/properties/EmbeddingServerProperties.kt` |
| OpenSearch 인덱스 매핑 | `job/infra/persistence/opensearch/JobSummaryIndexManager.kt` (`knnVectorProperty`) |
| OpenSearch 상수 | `job/infra/persistence/opensearch/JobSummaryIndexConstants.kt` (`EMBEDDING_VECTOR`) |
| 인덱싱 Consumer | `job/infra/kafka/consumer/JobSummaryIndexingConsumer.kt` (`handleIndex`) |
| 재인덱싱 Admin 서비스 | `job/application/summary/JobSummaryAdminService.kt` |
| 재인덱싱 Admin API | `job/presentation/controller/JobSummaryAdminController.kt` |
| OpenSearch Adapter | `job/infra/persistence/opensearch/JobSummaryOpenSearchAdapter.kt` |

### Python

| 역할 | 파일 |
|---|---|
| FastAPI 앱 | `src/embedding/main.py` |
| 모델 로더 | `src/embedding/model.py` |
| 라우터 | `src/embedding/router.py` |
| 설정 | `src/embedding/config.py` |

---

## 인덱싱 흐름

```
JobSummary 저장
  → Outbox → Debezium → Kafka
    → JobSummaryIndexingConsumer.handleIndex()
        → EmbeddingWebClientAdapter.embed() → POST /embed (Python FastAPI)
        → JobSummarySearchPayload.copy(embeddingVector = vector)
        → JobSummaryOpenSearchAdapter.index()
```

임베딩 실패 시:
- `embeddingVector = null`로 인덱싱 진행 (BM25 검색은 동작)
- 로그: `[JOB_SUMMARY_EMBEDDING_FAILED]`
- 복구: `POST /api/admin/job-summary/reindex-embedding`

---

## Admin API

### POST /api/admin/job-summary/reindex-all

인덱스 매핑 변경 시 사용.

```
기존 인덱스 삭제 + 재생성
  → DB 전체 커서 기반 조회 (batchSize 단위)
  → 임베딩 서버 호출
  → OpenSearch 인덱싱
```

- `?batchSize=50` (기본값, 최대 200)
- 응답: `{ "successCount": N }`
- 개별 문서 실패 시 로그 후 계속 진행

### POST /api/admin/job-summary/reindex-embedding

임베딩 서버 장애로 일부 문서 벡터 누락 시 사용.

```
OpenSearch에서 embeddingVector 없는 문서 조회
  → 임베딩 서버 호출
  → 부분 업데이트 (벡터만)
```

- `?batchSize=50` (기본값, 최대 500)
- 응답: `{ "successCount": N }`
- DB 조회 없음, OpenSearch → Python → OpenSearch

### 사용 순서

```
1. 인덱스 매핑 변경 시 → reindexAll 먼저
2. 이후 임베딩 서버 장애 복구 시 → reindexMissing
```

---

## 환경변수

### Spring (`docker/api/.env.*`)

```
EMBEDDING_SERVER_URL=http://preprocess:8000
```

### Python (`preprocess-pipeline/.env`)

```
EMBEDDING_SERVER_HOST=0.0.0.0
EMBEDDING_SERVER_PORT=8000
EMBEDDING_MODEL=jhgan/ko-sroberta-multitask
```

---

## OpenSearch 인덱스 재생성 주의사항

- `knn_vector` 필드는 생성 후 수정 불가 → 매핑 변경 시 반드시 `reindexAll` 호출
- `reindexAll`은 기존 인덱스 전체 삭제 → 호출 전 데이터 손실 감수 필요
- 재인덱싱 완료 전까지 k-NN 검색 불가 (BM25는 동작)
