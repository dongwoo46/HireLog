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
- k-NN k값: 기본 10 (응답 품질 보고 조정)
- RAG 응답 sources: `id` + `positionName` 포함
- Gemini Parser 먼저 → semanticRetrieval = true일 때만 Python 임베딩 호출 (순차)

---

## 전체 흐름

```
POST /api/rag/query { question: String }
  ↓
RagService
  ↓
[Step 1] GeminiRagParserAdapter
  - 질문 → RagQuery (intent, filters, semanticRetrieval, aggregation, baseline)
  ↓
[Step 2] 조건부 임베딩
  - semanticRetrieval = true  → EmbeddingWebClientAdapter.embed(질문) → vector
  - semanticRetrieval = false → skip
  ↓
[Step 3] RagQueryExecutor
  - intent별 실행 분기
  - DB cohort 조회 (applicationStatus 있을 경우)
  - OpenSearch k-NN(vector 사용) or aggregation 실행
  ↓
[Step 4] GeminiRagComposerAdapter
  - 실행 결과 → 자연어 응답
  ↓
RagAnswer { answer, intent, reasoning, evidences, sources }
```

---

## Intent별 실행 경로

| Intent | semanticRetrieval | 실행 경로 |
|---|---|---|
| `DOCUMENT_SEARCH` | true | embed(질문) → k-NN top-K → Composer |
| `SUMMARY` | true | embed(질문) → k-NN top-K → Composer |
| `PATTERN_ANALYSIS` | false | DB cohort → OpenSearch ids filter + aggregation + 텍스트 필드 조회 → Composer |
| `EXPERIENCE_ANALYSIS` | false | DB HiringStageRecord 조회 → Composer |
| `STATISTICS` | false | OpenSearch aggregation → Composer |
| `KEYWORD_SEARCH` | false | 기존 BM25 재사용 → Composer |

### k-NN vs aggregation 구분 원칙

- **k-NN 사용**: "비슷한 공고 찾아줘", "이런 직무 있어?" → 질문과 유사한 문서 N개 retrieval
- **aggregation 사용**: "합격한 공고 특징은?", "많이 나온 기술스택은?" → cohort 전체 집계
- k-NN으로 cohort 분석하면 전체가 아닌 일부만 보게 되어 분석 왜곡 발생

### PATTERN_ANALYSIS 상세 흐름

```
DB: memberId + saveType/stage/stageResult → jobSummaryId 목록 (전체)
  ↓
OpenSearch (두 가지 병렬):
  ① ids filter + techStackParsed terms aggregation
      → { Java: 8건, Kafka: 5건, ... }
  ② ids filter + 텍스트 필드 조회 (mustHaveSignals, technicalContext)
      → cohort 문서들의 원문 텍스트
  ↓
(baseline = true) OpenSearch: 필터 없이 동일 techStackParsed aggregation → 전체 분포
  ↓
서버 사이드: cohort 비율 / baseline 비율 → 배율 계산
  ↓
Gemini Composer:
  - aggregation 수치: "Kafka X%, 전체 대비 Y배"
  - 텍스트 필드: "많이 요구되는 능력 — 트래픽 처리, AI 모델 서빙 경험..."
```

### EXPERIENCE_ANALYSIS 상세 흐름

stage records (면접 기록, 코딩테스트 기록 등) 기반 개인 경험 분석.

```
DB: memberId + stage(선택) + stageResult(선택) → HiringStageRecord 목록
    (brandName, positionName, stage, note, result 포함)
  ↓
Gemini Composer: note 텍스트를 컨텍스트로 → 패턴/공통점 추출
```

사용 예시:
- "내가 받은 면접 질문 패턴 분석해줘" → stage=INTERVIEW_*, note 전달
- "합격한 면접 경험 공통점은?" → stageResult=PASSED + INTERVIEW_* notes
- "코딩테스트에서 어떤 유형이 많았어?" → stage=CODING_TEST, notes 전달

---

## 파일 구조

```
job/
├── application/
│   └── rag/
│       ├── RagService.kt                  ← 오케스트레이터 (Phase 4)
│       ├── model/
│       │   ├── RagQuery.kt               ✅ 완료
│       │   ├── RagFilters.kt             ✅ 완료
│       │   └── RagAnswer.kt              ✅ 완료 (reasoning/evidences 포함)
│       ├── port/
│       │   ├── RagLlmParser.kt           ✅ 완료
│       │   ├── RagLlmComposer.kt         ✅ 완료 (RagContext, RagDocument, RagStageRecord)
│       │   └── RagEmbedding.kt           ✅ 완료
│       └── executor/
│           └── RagQueryExecutor.kt       ← (Phase 3)
│
├── domain/type/
│   ├── CompanyDomain.kt                  ✅ 완료 (22개 enum)
│   └── CompanySize.kt                    ✅ 완료 (8개 enum, 스타트업 4단계)
│
├── infra/
│   ├── persistence/opensearch/
│   │   └── JobSummaryOpenSearchAdapter   ✅ searchByVector() 추가
│   └── external/gemini/
│       ├── GeminiClient.kt               ✅ generateContentWithSystemAsync() 추가
│       ├── GeminiPromptBuilder.kt        ✅ buildRagParserSystemInstruction/Prompt 추가
│       ├── GeminiRagParserAdapter.kt     ✅ 완료 (RagLlmParser 구현체)
│       └── GeminiRagComposerAdapter.kt   ← (Phase 4)
│
└── presentation/controller/
    └── RagController.kt                  ← (Phase 4)
```

---

## 핵심 모델

### RagQuery (LLM Parser 출력)

```kotlin
data class RagQuery(
    val intent: RagIntent,
    val semanticRetrieval: Boolean,
    val aggregation: Boolean,
    val baseline: Boolean,
    val filters: RagFilters,
    val parsedText: String
)
```

- intent: DOCUMENT_SEARCH / PATTERN_ANALYSIS / EXPERIENCE_ANALYSIS / STATISTICS / KEYWORD_SEARCH / SUMMARY
- semanticRetrieval: true/false — 임베딩 호출 여부 결정
- aggregation: true/false
- baseline: true/false
- filters
  - cohort용: saveType(SAVED/APPLY), stage, stageResult(PASSED/FAILED/PENDING)
  - OpenSearch 필터용: careerType, techStacks, brandName, companyDomain, dateRange

### RagAnswer

```kotlin
data class RagAnswer(
    val answer: String,
    val intent: RagIntent,
    val reasoning: String?,           // LLM 사고 과정/근거 요약
    val evidences: List<RagEvidence>?, // 구체적 근거 목록
    val sources: List<RagSource>?      // k-NN 결과 (DOCUMENT_SEARCH/SUMMARY만)
)

data class RagSource(val id: Long, val positionName: String)

data class RagEvidence(
    val type: RagEvidenceType,   // DOCUMENT / AGGREGATION / EXPERIENCE
    val summary: String,
    val detail: String?,
    val sourceId: Long?
)
```

---

## OpenSearch 추가 사항

### JobSummaryOpenSearchAdapter.searchByVector()

k-NN 쿼리 + filter(careerType, companyDomain 등) 조합. 반환 필드:
- 기본: id, score, brandName, positionName, companyDomain, companySize
- JD: responsibilities, requiredQualifications, preferredQualifications, techStackParsed
- Insight: idealCandidate, mustHaveSignals, technicalContext, preparationFocus

---

## CompanyDomain / CompanySize Enum

### CompanyDomain (22개)

| 값 | 설명 |
|---|---|
| FINTECH | 결제, 뱅킹, 인슈어테크, 증권 |
| E_COMMERCE | 온라인 쇼핑, 마켓플레이스 |
| FOOD_DELIVERY | 배달 앱, 음식 주문 플랫폼 |
| LOGISTICS | 풀필먼트, 라스트마일, B2B 물류 |
| MOBILITY | 차량공유, 킥보드, 대중교통 데이터 |
| HEALTHCARE | 의료 AI, 원격진료, 디지털 헬스 |
| EDTECH | 온라인 학습, LMS, 어학 |
| GAME | 모바일/PC/콘솔 게임 |
| MEDIA_CONTENT | OTT, 스트리밍, 음악, 웹툰 |
| SOCIAL_COMMUNITY | SNS, 데이팅앱, 커뮤니티 |
| TRAVEL_ACCOMMODATION | 항공, 호텔, OTA |
| REAL_ESTATE | 부동산 플랫폼, 프롭테크 |
| HR_RECRUITING | 채용 플랫폼, HR 관리 |
| AD_MARKETING | 애드테크, 마케팅 자동화 |
| AI_ML | AI 솔루션, MLOps, 데이터 플랫폼 |
| CLOUD_INFRA | 클라우드 서비스, DevOps 툴 |
| SECURITY | 사이버보안, 인증, 접근제어 |
| ENTERPRISE_SW | ERP, CRM, B2B SaaS |
| BLOCKCHAIN_CRYPTO | 블록체인, 암호화폐, Web3 |
| MANUFACTURING_IOT | 스마트팩토리, 산업 자동화 |
| PUBLIC_SECTOR | 공기업, 정부 SI |
| OTHER | fallback (파싱 실패 포함) |

### CompanySize (8개, 스타트업 4단계 세분화)

| 값 | 설명 |
|---|---|
| SEED | Pre-Seed/Seed, ~10명 |
| EARLY_STARTUP | Series A, 10~50명 |
| GROWTH_STARTUP | Series B~C, 50~300명 |
| SCALE_UP | Series C+/유니콘급 300명+, 예: 토스·당근·컬리 |
| MID_SIZED | 상장 중소/중견기업 |
| LARGE_CORP | 대기업, 예: 카카오·네이버·삼성SDS |
| FOREIGN_CORP | 외국계, 예: Google Korea·Amazon |
| UNKNOWN | fallback (판단 불가 포함) |

- Assembler에서 LLM 파싱 실패 시: companyDomain → OTHER, companySize → UNKNOWN
- DB 컬럼: NOT NULL DEFAULT 'OTHER' / 'UNKNOWN' (V12 마이그레이션)

---

## LLM Parser (GeminiRagParserAdapter)

질문 → JSON (intent, filters, semanticRetrieval, aggregation, baseline) 반환
JSON 파싱 실패 시 → KEYWORD_SEARCH fallback

GeminiClient에 `generateContentWithSystemAsync(systemInstruction, prompt)` 오버로드 추가.
JD 요약과 다른 system instruction 주입 가능.

---

## DB 조회

### cohort 조회 (PATTERN_ANALYSIS)

MemberJobSummaryQuery.findJobSummaryIdsByCohort() 추가 필요
- 입력: memberId, saveType, stage, stageResult
- 출력: jobSummaryId 목록

cohort 조합:
- saveType=SAVED → 저장한 공고
- saveType=APPLY → 지원 의사 공고
- stage=DOCUMENT + stageResult=PASSED → 서류합격 공고
- stageResult=FAILED → 불합격 공고

### stage records 조회 (EXPERIENCE_ANALYSIS)

MemberJobSummaryQuery.findStageRecordsForRag() 추가 필요
- 입력: memberId, stage(선택), stageResult(선택)
- 출력: brandName, positionName, stage, note, result

---

## 구현 순서

### ✅ Phase 0 — JobSummary 필드 선행 추가 (완료)

- `companyDomain: CompanyDomain` (non-null, fallback=OTHER) — 22개 enum
- `companySize: CompanySize` (non-null, fallback=UNKNOWN) — 8개 enum (스타트업 4단계)
- Gemini 프롬프트: 영문 enum 이름 직접 출력하도록 변경
- Flyway V12: NOT NULL DEFAULT 추가
- `JobSummaryLlmRawResult` / `JobSummaryLlmResult` / `JobSummary` 엔티티 수정
- `JobSummarySearchPayload` / `JobSummaryOutboxPayload` / `JobSummaryIndexConstants` / `JobSummaryIndexManager` 수정

---

### ✅ Phase 1 — k-NN 검색 기반 구축 (완료)

- `JobSummaryOpenSearchAdapter.searchByVector()` 추가
  - k-NN 쿼리 + filter(careerType, companyDomain 등) 조합

---

### ✅ Phase 2 — RAG 모델 및 Parser 구현 (완료)

- `RagQuery`, `RagFilters`, `RagAnswer`(reasoning/evidences 포함), `RagSource`, `RagEvidence` 모델
- `RagLlmParser` / `RagLlmComposer` / `RagEmbedding` 포트
- `GeminiRagParserAdapter` — intent 분류 + filter 추출 + KEYWORD_SEARCH fallback
- `GeminiClient.generateContentWithSystemAsync()` 오버로드 추가
- `GeminiPromptBuilder.buildRagParserSystemInstruction/Prompt()` 추가

---

### Phase 3 — Executor 구현 (intent별)

1. `RagQueryExecutor` 구현
   - `DOCUMENT_SEARCH` / `SUMMARY`: embed → k-NN
   - `PATTERN_ANALYSIS`: DB cohort → OpenSearch aggregation + 텍스트 조회
   - `EXPERIENCE_ANALYSIS`: DB HiringStageRecord 조회
   - `STATISTICS`: OpenSearch aggregation only
   - `KEYWORD_SEARCH`: 기존 BM25 재사용
2. `MemberJobSummaryQuery.findJobSummaryIdsByCohort()` 포트 추가 + 어댑터 구현
3. `MemberJobSummaryQuery.findStageRecordsForRag()` 포트 추가 + 어댑터 구현

---

### Phase 4 — Composer 및 서비스 조립

1. `GeminiRagComposerAdapter` 구현 (`RagLlmComposer` 포트)
2. `RagService` 조립 (Parser → 조건부 임베딩 → Executor → Composer)
3. `RagController` + `POST /api/rag/query` 노출
