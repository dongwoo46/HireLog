# RAG 서비스

## 개요

자연어 질문 → LLM Parser → 실행 분기 → OpenSearch/DB 실행 → LLM 응답 생성

검색 기능(BM25)과 완전 분리된 별도 서비스.

---

## 확정 사항

- 기존 BM25 검색에 k-NN 추가 안 함 (페이지네이션 충돌, 역할 분리)
- k-NN은 RAG 전용으로만 사용
- LLM Parser 필수 (검색 + 분석 모두 처리해야 하므로 intent 분류 필요)
- 임베딩 모델: `jhgan/ko-sroberta-multitask` (768차원, 기존 인덱스와 동일)
- LLM: Gemini (기존 `JobSummaryLlm`과 동일 클라이언트)
- k-NN k값: 기본 10, hybrid 후보 각 20개 → RRF → top 10
- RAG 응답 sources: `id` + `brandName` + `positionName` 포함
- Gemini Parser 먼저 → semanticRetrieval = true일 때만 Python 임베딩 호출 (순차)
- RAG 전 파이프라인(질문 → 파서 → 실행 → 응답) 전 과정 DB 저장 (`rag_query_log`)
- USER 일일 3회 제한 (Redis INCR), ADMIN 무제한
- Parser 파싱 실패 fallback → `DOCUMENT_SEARCH`

---

## 전체 흐름

```
POST /api/rag/query { question: String }
  ↓
RagService (RateLimiter 체크)
  ↓
[Step 1] GeminiRagParserAdapter
  - 질문 → RagQuery (intent, filters, semanticRetrieval, aggregation, baseline)
  - 파싱 실패 시 DOCUMENT_SEARCH fallback (예외 전파 없음)
  ↓
[Step 2] 조건부 임베딩
  - semanticRetrieval = true  → EmbeddingWebClientAdapter.embed(질문) → vector
  - semanticRetrieval = false → skip
  ↓
[Step 3] RagQueryExecutor
  - intent별 실행 분기 (아래 참고)
  ↓
[Step 4] GeminiRagComposerAdapter
  - RagContext → 마크다운 직렬화 → Gemini → RagAnswer
  ↓
RagService → DB 저장 (RagQueryLog, 실패해도 응답 반환)
  ↓
RagAnswer { answer, intent, reasoning, evidences, sources }
```

---

## Intent별 실행 경로

| Intent | semanticRetrieval | 실행 경로 |
|---|---|---|
| `DOCUMENT_SEARCH` | true | embed(질문) → Hybrid(k-NN+BM25) → RRF → top 10 → Composer |
| `SUMMARY` | true | 동일 |
| `STATISTICS` | false | [cohort 없음] OpenSearch aggregation → Composer |
| `STATISTICS` | false | [cohort 있음] DB cohort ids → aggregation + Feature Extractor → Composer |
| `EXPERIENCE_ANALYSIS` | false | DB HiringStageRecord + JobSummaryReview → Composer |

### k-NN vs aggregation 구분 원칙

- **k-NN 사용**: "비슷한 공고 찾아줘", "이런 직무 있어?" → 질문과 유사한 문서 N개 retrieval
- **aggregation 사용**: "합격한 공고 특징은?", "많이 나온 기술스택은?" → cohort 전체 집계
- k-NN으로 cohort 분석하면 전체가 아닌 일부만 보게 되어 분석 왜곡 발생

### STATISTICS 상세 흐름

```
[cohort 조건 있을 때]
DB: memberId + saveType/stage/stageResult → jobSummaryId 목록
  ↓
OpenSearch:
  ① ids filter + techStackParsed / companyDomain / companySize aggregation
  ② (baseline=true) 필터 없이 동일 aggregation → 전체 분포
  ↓
서버 사이드: cohort 비율 / baseline 비율 → 배율(multiplier) 계산
  ↓
cohort ids → findCohortDocumentTexts → 전처리 → Gemini Feature Extractor
  → 정성적 특징 레이블 추출 (기술스택/언어명 제외, 업무환경/특성만)
  → keyword 매칭 → observedCount + snippets 계산
  ↓
Gemini Composer: AggregationEntry + TextFeature → 자연어 답변
```

### EXPERIENCE_ANALYSIS 상세 흐름

```
DB (두 소스 병렬 조회):
  ① HiringStageRecord: memberId + stage(선택) + stageResult(선택)
     → brandName, positionName, stage, note, result
  ② JobSummaryReview: memberId
     → brandName, positionName, hiringStage, difficultyRating, satisfactionRating,
        prosComment, consComment, tip

Gemini Composer:
  - 두 소스를 통합해 패턴/테마 분석 (단순 나열 금지)
  - 반복되는 어려움, 면접 스타일, 자주 나온 주제 등 정성적 인사이트 추출
```

사용 예시:
- "내가 받은 면접 질문 패턴 분석해줘" → stage=INTERVIEW_*, notes 기반 패턴 분석
- "합격한 면접 경험 공통점은?" → stageResult=PASSED, notes + reviews 분석
- "코딩테스트에서 어떤 유형이 많았어?" → stage=CODING_TEST, notes 전달

---

## 파일 구조

```
job/
├── application/
│   └── rag/
│       ├── RagService.kt                       ✅ 오케스트레이터 (RateLimiter → Parser → Executor → Composer → DB저장)
│       ├── RagRateLimiter.kt                   ✅ Redis INCR, USER 3회/일
│       ├── RagLogReadService.kt                ✅ RAG 로그 조회 서비스
│       ├── model/
│       │   ├── RagQuery.kt                     ✅ Parser 출력 (intent, filters, semanticRetrieval, aggregation, baseline)
│       │   ├── RagFilters.kt                   ✅ cohort + OpenSearch 필터
│       │   ├── RagIntent.kt                    ✅ 4개 enum (DOCUMENT_SEARCH / SUMMARY / STATISTICS / EXPERIENCE_ANALYSIS)
│       │   └── RagAnswer.kt                    ✅ reasoning / evidences / sources (brandName 포함)
│       ├── port/
│       │   ├── RagLlmParser.kt                 ✅ 질문 → RagQuery
│       │   ├── RagLlmComposer.kt               ✅ RagContext → RagAnswer
│       │   │                                      (RagContext / RagDocument / AggregationEntry / TextFeature /
│       │   │                                       RagStageRecord / RagReviewRecord 정의)
│       │   ├── RagLlmFeatureExtractor.kt       ✅ 전처리 텍스트 → 특징 레이블 (업무환경/특성만, 기술스택명 제외)
│       │   ├── RagEmbedding.kt                 ✅ 텍스트 → 768차원 벡터
│       │   ├── RagCohortQuery.kt               ✅ cohort ids / stage records / reviews 조회
│       │   ├── RagQueryLogCommand.kt           ✅ 로그 저장 포트
│       │   └── RagQueryLogQuery.kt             ✅ 로그 조회 포트 (페이징, 필터)
│       ├── view/
│       │   └── RagQueryLogView.kt              ✅ 로그 조회 Read model
│       └── executor/
│           └── RagQueryExecutor.kt             ✅ intent별 실행 분기 + 모든 수치 계산
│
├── domain/
│   ├── model/
│   │   └── RagQueryLog.kt                      ✅ 파이프라인 4단계 전 과정 저장 엔티티 (immutable)
│   └── type/
│       ├── CompanyDomain.kt                    ✅ 22개 enum
│       └── CompanySize.kt                      ✅ 8개 enum
│
├── infra/
│   ├── persistence/
│   │   ├── jpa/
│   │   │   ├── RagCohortQueryJpaAdapter.kt     ✅ cohort ids + stage records + reviews 조회
│   │   │   ├── RagQueryLogJpaAdapter.kt        ✅ Command 구현체
│   │   │   ├── RagQueryLogJpaQueryAdapter.kt   ✅ Query 구현체 (JpaSpecificationExecutor)
│   │   │   └── repository/
│   │   │       └── RagQueryLogJpaRepository.kt ✅ JpaSpecificationExecutor 포함
│   │   └── opensearch/
│   │       └── JobSummaryOpenSearchAdapter     ✅ searchHybrid / aggregateFields / findCohortDocumentTexts
│   └── external/gemini/
│       ├── GeminiClient.kt                     ✅ generateContentWithSystemAsync() 오버로드
│       ├── GeminiPromptBuilder.kt              ✅ Parser / FeatureExtractor / Composer 프롬프트 빌더
│       ├── GeminiRagParserAdapter.kt           ✅ RagLlmParser 구현, DOCUMENT_SEARCH fallback
│       ├── GeminiRagFeatureExtractorAdapter.kt ✅ RagLlmFeatureExtractor 구현
│       └── GeminiRagComposerAdapter.kt         ✅ RagContext 직렬화 → Gemini → RagAnswer
│
├── resources/db/migration/
│   └── V13__create_rag_query_log.sql           ✅ rag_query_log 테이블 + 인덱스 3개
│
└── presentation/controller/
    ├── RagController.kt                        ✅ POST /api/rag/query, GET /api/rag/logs
    ├── RagAdminController.kt                   ✅ POST /api/admin/rag/parse, GET /api/admin/rag/logs, GET /api/admin/rag/logs/{id}
    └── dto/response/
        ├── RagAnswerRes.kt                     ✅
        └── RagQueryLogRes.kt                   ✅
```

---

## 핵심 모델

### RagQuery (LLM Parser 출력)

```kotlin
data class RagQuery(
    val intent: RagIntent,
    val semanticRetrieval: Boolean,  // true → embed 호출
    val aggregation: Boolean,        // true → OpenSearch aggregation
    val baseline: Boolean,           // true → 전체 분포와 비교
    val filters: RagFilters,
    val parsedText: String
)
```

filters:
- cohort용: `saveType`(SAVED/APPLY), `stage`, `stageResult`(PASSED/FAILED/PENDING)
- OpenSearch 필터용: `careerType`, `techStacks`, `brandName`, `companyDomain`, `dateRange`

### RagContext (Executor → Composer)

```kotlin
data class RagContext(
    val documents: List<RagDocument> = emptyList(),       // DOCUMENT_SEARCH / SUMMARY
    val aggregations: List<AggregationEntry> = emptyList(), // STATISTICS
    val textFeatures: List<TextFeature> = emptyList(),    // STATISTICS + cohort
    val stageRecords: List<RagStageRecord> = emptyList(), // EXPERIENCE_ANALYSIS
    val reviewRecords: List<RagReviewRecord> = emptyList() // EXPERIENCE_ANALYSIS
)
```

### RagAnswer (최종 응답)

```kotlin
data class RagAnswer(
    val answer: String,
    val intent: RagIntent,
    val reasoning: String?,
    val evidences: List<RagEvidence>?,
    val sources: List<RagSource>?  // DOCUMENT_SEARCH / SUMMARY만
)

data class RagSource(val id: Long, val brandName: String, val positionName: String)

data class RagEvidence(
    val type: RagEvidenceType,   // DOCUMENT / AGGREGATION / EXPERIENCE
    val summary: String,
    val detail: String?,
    val sourceId: Long?
)
```

---

## DB 조회 (RagCohortQuery 포트)

| 메서드 | 용도 |
|---|---|
| `findJobSummaryIdsByCohort(memberId, saveType, stage, stageResult)` | STATISTICS cohort ids |
| `findStageRecordsForRag(memberId, stage, stageResult)` | EXPERIENCE_ANALYSIS stage notes |
| `findReviewsByMemberId(memberId)` | EXPERIENCE_ANALYSIS reviews |

cohort 조합 예시:
- saveType=SAVED → 저장한 공고
- saveType=APPLY → 지원 의사 공고
- stage=DOCUMENT + stageResult=PASSED → 서류합격 공고
- stageResult=FAILED → 불합격 공고

---

## LLM 호출 수

| Intent | 호출 수 | 내역 |
|---|---|---|
| DOCUMENT_SEARCH / SUMMARY | 2회 | Parser + Composer |
| STATISTICS (cohort 없음) | 2회 | Parser + Composer |
| STATISTICS (cohort 있음) | 3회 | Parser + Feature Extractor + Composer |
| EXPERIENCE_ANALYSIS | 2회 | Parser + Composer |

---

## 구현 완료 이력

### Phase 0 — JobSummary 필드 선행 추가
- `companyDomain: CompanyDomain` (non-null, fallback=OTHER) — 22개 enum
- `companySize: CompanySize` (non-null, fallback=UNKNOWN) — 8개 enum
- Flyway V12, Assembler fallback

### Phase 1 — k-NN 검색 기반 구축
- `JobSummaryOpenSearchAdapter.searchByVector()` 추가

### Phase 2 — RAG 모델 및 Parser 구현
- 도메인 모델, 포트 정의
- `GeminiRagParserAdapter` — intent 분류 + filter 추출

### Phase 3 — Executor 구현
- `RagQueryExecutor` — intent별 분기, 수치 계산
- `JobSummaryOpenSearchAdapter`: searchHybrid / aggregateFields / findCohortDocumentTexts
- `RagCohortQueryJpaAdapter` — cohort ids, stage records, reviews
- `GeminiRagFeatureExtractorAdapter` — 특징 레이블 추출 (업무환경/특성만)

### Phase 4 — Composer 및 서비스 조립
- `GeminiRagComposerAdapter` — RagContext 직렬화 → Gemini → RagAnswer
- `RagService` — 전 파이프라인 조립 + DB 저장
- `RagController`, `RagAdminController`

### Phase 5 — DB 로깅 및 조회 API
- `V13__create_rag_query_log.sql`
- `RagQueryLog` 엔티티 (4단계 전 결과 JSON TEXT 저장)
- `RagLogReadService` — searchAdmin / searchMine
- `GET /api/rag/logs`, `GET /api/admin/rag/logs[/{id}]`
