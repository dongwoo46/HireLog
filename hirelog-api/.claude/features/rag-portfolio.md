# RAG 기반 채용공고 AI 어시스턴트

## 1. 프로젝트 개요

HireLog는 취업 준비생이 채용공고를 저장·분석·관리하는 서비스다.  
RAG(Retrieval-Augmented Generation) 어시스턴트는 그 핵심 AI 기능으로,  
유저가 자연어로 질문하면 자신의 저장 공고·면접 기록·리뷰·전체 공고 데이터를 기반으로 정확한 답변을 생성한다.

**예시 질문:**
- "내가 저장한 핀테크 공고에서 가장 많이 요구하는 기술스택은?"
- "Kafka 쓰는 백엔드 공고 찾아줘"
- "면접 본 곳들의 공통점이 뭐야?"
- "합격한 공고가 전체 평균 대비 어떤 특징이 더 많아?"

---

## 2. 기술 스택

| 분류 | 기술 |
|---|---|
| 백엔드 | Spring Boot, Kotlin |
| 검색 엔진 | OpenSearch (BM25 + k-NN) |
| 임베딩 모델 | `jhgan/ko-sroberta-multitask` (768차원, Python 서버) |
| LLM | Google Gemini |
| DB | PostgreSQL |
| 캐시 / Rate Limit | Redis |

---

## 3. 아키텍처

### 헥사고날 아키텍처 + 파이프라인 패턴

```
[Controller]
    ↓ question: String
[RagService] ← RateLimiter (Redis, USER 3회/일)
    ↓
[Step 1] RagLlmParser Port ← GeminiRagParserAdapter
    ↓ RagQuery { intent, filters, semanticRetrieval, ... }
[Step 2] 조건부 임베딩 (semanticRetrieval=true 시)
    ↓ RagEmbedding Port → Python 임베딩 서버 → vector
[Step 3] RagQueryExecutor
    ├── DOCUMENT_SEARCH / SUMMARY
    │     ↓ OpenSearch Hybrid (k-NN + BM25) → RRF → top 10
    │
    ├── STATISTICS
    │     ↓ RagCohortQuery Port → PostgreSQL (cohort ids)
    │     ↓ OpenSearch aggregation (techStack / companyDomain / companySize)
    │     ↓ [cohort 있을 때] RagLlmFeatureExtractor Port → Gemini
    │     ↓ keyword 매칭 → observedCount + snippets 계산
    │
    └── EXPERIENCE_ANALYSIS
          ↓ RagCohortQuery Port → PostgreSQL
              ① HiringStageRecord (전형 단계 노트)
              ② JobSummaryReview (공고 리뷰 — 장단점/난이도/만족도)
    ↓ RagContext (구조화된 결과)
[Step 4] RagLlmComposer Port ← GeminiRagComposerAdapter
    ↓ RagAnswer { answer, intent, reasoning, evidences, sources }
[RagService] → DB 저장 (RagQueryLog: 4단계 전 과정, 실패해도 응답 반환)
[Controller]
```

### 의존성 방향 (헥사고날)

```
도메인 모델 (RagQuery, RagContext, RagAnswer)
    ↑ 포트 (RagLlmParser, RagLlmComposer, RagEmbedding, ...)
        ↑ 어댑터 (GeminiRagParserAdapter, JobSummaryOpenSearchAdapter, ...)
```

외부 의존성(Gemini, OpenSearch, Python, PostgreSQL, Redis)은 모두 포트 뒤에 숨겨진다.  
비즈니스 로직(`RagQueryExecutor`)은 외부 기술에 무관하게 테스트 가능하다.

---

## 4. Intent 분류 및 실행 경로

LLM(Gemini)이 유저 질문을 4가지 Intent로 분류한다.

| Intent | 판단 기준 | 실행 경로 | LLM 호출 수 |
|---|---|---|---|
| `DOCUMENT_SEARCH` | 공고 검색/탐색 요청 | Hybrid(k-NN+BM25) → RRF | 2회 |
| `SUMMARY` | 공고 내용 요약 요청 | 동일 | 2회 |
| `STATISTICS` | 통계/집계 요청 (cohort 없음) | OpenSearch aggregation | 2회 |
| `STATISTICS` | 통계/집계 요청 (cohort 있음) | DB cohort → aggregation + Feature Extractor | **3회** |
| `EXPERIENCE_ANALYSIS` | 면접/전형 경험 분석 | DB HiringStageRecord + JobSummaryReview | 2회 |

Parser fallback: 파싱 실패 시 예외 전파 없이 `DOCUMENT_SEARCH`로 처리.

---

## 5. 핵심 기술 구현 상세

### 5-1. Hybrid Search (k-NN + BM25 + RRF)

단순 k-NN 또는 BM25만 사용하면 각각 의미 유사도와 키워드 정확도 중 하나가 약하다.  
두 방식의 결과를 **Reciprocal Rank Fusion(RRF)** 으로 결합해 보완한다.

```
k-NN 후보 20개 + BM25 후보 20개
          ↓
RRF 점수 = Σ 1 / (k + rank_i),  k=60
          ↓
상위 10개 반환
```

기존 BM25 검색 페이지네이션과 충돌 방지를 위해 RAG 전용 별도 메서드(`searchHybrid`)로 분리.

### 5-2. STATISTICS cohort 분석 — 배율(multiplier) 계산

"내가 합격한 공고가 전체 평균 대비 Kafka를 얼마나 더 요구해?"를 정량으로 답하기 위해:

```
cohortRatio[label] = cohortCount[label] / cohortTotal
baselineRatio[label] = baselineCount[label] / baselineTotal

multiplier = cohortRatio / baselineRatio
```

- multiplier > 1.0 → cohort가 전체보다 해당 기술 요구 비율이 높음
- baseline에 없는 label → multiplier = null (전파 안 함)
- LLM에 raw 수치 전달 금지 — Executor가 계산 완료 후 구조화된 `AggregationEntry`로만 전달 (hallucination 방지)

### 5-3. LLM Feature Extractor — 역할 분리

cohort 문서에서 정성적 특징을 추출할 때 LLM과 시스템의 역할을 명확히 분리했다.

| 역할 | 담당 |
|---|---|
| 레이블 추출 ("대용량 트래픽 처리", "MSA 기반 설계") | Gemini (LLM) |
| 레이블별 관찰 건수 계산 | `RagQueryExecutor` (keyword 매칭) |
| 대표 스니펫 추출 | `RagQueryExecutor` (앞뒤 40자 컨텍스트) |
| 근거 문서 ID 목록 | `RagQueryExecutor` |

**기술스택/언어명 추출 금지** — Java, Kafka 같은 이름은 aggregation에서 이미 집계됨.  
Feature Extractor는 업무 환경·특성("글로벌 서비스 운영", "온콜 및 장애 대응")만 추출.  
LLM이 count를 직접 세거나 ID를 생성하면 hallucination 발생 가능.

### 5-4. 문서 전처리 (Feature Extractor 입력)

LLM에 보내는 cohort 문서는 토큰 절약 + 노이즈 제거를 위해 전처리한다.

```
사용 필드: responsibilities, requiredQualifications, preferredQualifications, techStackParsed
제외 필드: idealCandidate, mustHaveSignals, technicalContext (LLM 가공 insight → 편향 방지)

필드별 최대 길이: responsibilities 200자 / requiredQualifications 200자 /
               preferredQualifications 150자 / techStack 100자
공백 normalize: 연속 공백·줄바꿈 → 단일 공백
문서 전체 soft limit: 600자
```

### 5-5. Rate Limiter (Redis INCR + 자정 TTL)

멀티 인스턴스 환경에서도 안전한 일일 호출 제한을 위해 Redis atomic INCR을 사용한다.

```
key = "rag:rate:limit:{memberId}:{today}"
count = INCR key

if count == 1:  # 첫 호출
    EXPIRE key (자정까지 남은 초)

if count > 3:
    throw BusinessException(RAG_RATE_LIMIT_EXCEEDED)  # HTTP 429
```

- ADMIN은 제한 없음 (LLM 비용 모니터링 및 QA용)
- `synchronized` 없이 Redis 원자성으로 race condition 방지

### 5-6. Composer — context 직렬화 전략

Gemini Composer에 전달하는 프롬프트는 `RagContext`를 구조화된 마크다운 텍스트로 변환한다.

```
## 검색된 공고 (10건)
[1] id=42 | 토스 | Backend Engineer
  도메인: FINTECH | 규모: SCALE_UP
  기술스택: Kotlin, Spring Boot, Kafka
  주요 업무: 결제 API 개발 ...

## 집계 결과
### techStack
  Kafka: 8건 (전체 대비 3.3x)
  Spring Boot: 12건

## 정성적 특징
- 대용량 트래픽 처리 (6건 관찰)
  > 대용량 트래픽 환경에서 MSA 기반 ...

## 전형 경험 기록 (3건)
### 토스 — Backend Engineer
전형 단계: INTERVIEW_1 | 결과: PASSED
기록 내용:
시스템 설계 질문 위주였으며 ...

## 공고 리뷰 (2건)
### 카카오 — Backend Developer
전형 단계: INTERVIEW_2 | 난이도: 8/10 | 만족도: 7/10
장점: 코드 리뷰 문화가 잘 갖춰져 있음
단점: 프로세스가 길고 피드백이 느림
```

Composer(LLM)는 이 텍스트만 보고 자연어 답변을 생성한다.  
raw 수치·ID 계산은 Executor 단계에서 완료되므로 Composer는 해석과 패턴 분석만 담당한다.

### 5-7. RAG 파이프라인 전 과정 DB 저장

답변 재현 가능성 확보 및 프롬프트 품질 개선을 위해 4단계 전 과정을 `rag_query_log` 테이블에 저장한다.

| 컬럼 | 내용 |
|---|---|
| `question` | 사용자 질문 원본 |
| `intent`, `parsed_text`, `parsed_filters_json` | LLM Parser 추출 결과 |
| `context_json` | Executor 실행 결과 (문서/집계/경험 기록) |
| `answer`, `reasoning`, `evidences_json`, `sources_json` | Composer 최종 결과 |

- 저장 실패해도 응답은 정상 반환 (`runCatching` 처리)
- JSON 필드는 TEXT 컬럼에 raw JSON 저장 — 스키마 변경 없이 구조 변경 가능

---

## 6. 설계 결정 기록

### BM25 기존 검색과 RAG 검색을 분리한 이유

기존 채용공고 검색은 BM25 + 페이지네이션(lastId 커서 방식).  
k-NN 검색은 OpenSearch의 페이지네이션 방식과 충돌하며,  
두 기능의 역할(브라우징 vs 질의응답)이 다르므로 완전히 분리했다.

### KEYWORD_SEARCH / PATTERN_ANALYSIS intent를 제거한 이유

- `KEYWORD_SEARCH`: `DOCUMENT_SEARCH`의 Hybrid(k-NN+BM25)가 이미 BM25를 포함하므로 불필요.
- `PATTERN_ANALYSIS`: `STATISTICS`가 cohort 조건 유무에 따라 동일한 역할을 수행하므로 통합.  
  Intent 수를 줄여 Parser의 분류 정확도를 높이는 효과도 있음.

### LLM Parser 파싱 실패 시 예외를 전파하지 않는 이유

RAG는 탐색적 질의응답 기능이다.  
파싱 실패가 유저에게 500 에러로 보이는 것보다,  
`DOCUMENT_SEARCH` fallback으로 부분적으로라도 답변을 제공하는 것이 UX 상 낫다.

### EXPERIENCE_ANALYSIS에 두 데이터 소스를 사용하는 이유

- `HiringStageRecord`: 전형 단계별 사용자 노트 — 면접 질문, 분위기, 난이도 등 주관적 경험
- `JobSummaryReview`: 공고 리뷰 — 구조화된 장단점/평점 데이터

두 소스를 통합하면 Composer가 더 풍부한 컨텍스트로 패턴을 분석할 수 있다.

---

## 7. 파일 구조 (전체)

```
job/
├── application/rag/
│   ├── RagService.kt                           ← 오케스트레이터 (+ 파이프라인 전 과정 DB 저장)
│   ├── RagRateLimiter.kt                       ← Redis 일일 호출 제한
│   ├── RagLogReadService.kt                    ← RAG 로그 조회 서비스
│   ├── model/
│   │   ├── RagQuery.kt                         ← Parser 출력 (intent, filters, ...)
│   │   ├── RagFilters.kt                       ← cohort + OpenSearch 필터
│   │   ├── RagIntent.kt                        ← DOCUMENT_SEARCH / SUMMARY / STATISTICS / EXPERIENCE_ANALYSIS
│   │   └── RagAnswer.kt                        ← 최종 응답 (answer, reasoning, evidences, sources+brandName)
│   ├── port/
│   │   ├── RagLlmParser.kt                     ← 질문 → RagQuery
│   │   ├── RagLlmComposer.kt                   ← RagContext → RagAnswer
│   │   │                                          (RagContext / RagDocument / AggregationEntry / TextFeature /
│   │   │                                           RagStageRecord / RagReviewRecord 정의)
│   │   ├── RagLlmFeatureExtractor.kt           ← 전처리 텍스트 → 특징 레이블
│   │   ├── RagEmbedding.kt                     ← 텍스트 → 768차원 벡터
│   │   ├── RagCohortQuery.kt                   ← cohort ids / stage records / reviews 조회
│   │   ├── RagQueryLogCommand.kt               ← 로그 저장 포트
│   │   └── RagQueryLogQuery.kt                 ← 로그 조회 포트 (페이징, 필터)
│   ├── view/
│   │   └── RagQueryLogView.kt                  ← 로그 조회 Read model
│   └── executor/
│       └── RagQueryExecutor.kt                 ← intent별 실행 분기 + 모든 수치 계산
│
├── domain/model/
│   └── RagQueryLog.kt                          ← 파이프라인 4단계 전 과정 저장 엔티티
│
├── infra/
│   ├── persistence/
│   │   ├── jpa/
│   │   │   ├── RagCohortQueryJpaAdapter.kt     ← cohort ids + stage records + reviews 조회
│   │   │   ├── RagQueryLogJpaAdapter.kt        ← Command 구현체
│   │   │   └── RagQueryLogJpaQueryAdapter.kt   ← Query 구현체 (JpaSpecificationExecutor)
│   │   └── opensearch/
│   │       └── JobSummaryOpenSearchAdapter.kt  ← searchHybrid / aggregateFields / findCohortDocumentTexts
│   └── external/gemini/
│       ├── GeminiRagParserAdapter.kt           ← RagLlmParser 구현, DOCUMENT_SEARCH fallback
│       ├── GeminiRagFeatureExtractorAdapter.kt ← RagLlmFeatureExtractor 구현
│       └── GeminiRagComposerAdapter.kt         ← RagLlmComposer 구현, context 직렬화
│
├── resources/db/migration/
│   └── V13__create_rag_query_log.sql           ← rag_query_log 테이블 + 인덱스
│
└── presentation/controller/
    ├── RagController.kt                        ← POST /api/rag/query, GET /api/rag/logs
    └── RagAdminController.kt                   ← POST /api/admin/rag/parse, GET /api/admin/rag/logs[/{id}]

test/
├── job/application/rag/
│   ├── RagQueryExecutorTest.kt                 ← 집계 multiplier, textFeature, intent 분기
│   └── RagRateLimiterTest.kt                   ← Redis INCR 제한, admin 제외
└── job/infra/external/gemini/
    └── GeminiRagParserAdapterTest.kt           ← JSON 파싱, fallback 동작
```

---

## 8. 핵심 성과 요약 (포트폴리오용)

- 자연어 질문 → 4가지 intent 자동 분류 → intent별 최적 데이터 소스 선택 파이프라인 설계
- Hybrid Search(k-NN + BM25 + RRF)로 의미 유사도와 키워드 정확도를 동시에 확보
- LLM hallucination 방지: Feature Extractor는 업무환경 레이블만 추출, count/snippet/ID는 시스템이 직접 계산
- EXPERIENCE_ANALYSIS: 전형 노트(HiringStageRecord) + 공고 리뷰(JobSummaryReview) 통합 → 다각도 경험 패턴 분석
- 헥사고날 아키텍처로 Gemini/OpenSearch/Python 서버를 포트 뒤에 격리 → 단위 테스트 가능
- Redis atomic INCR으로 멀티 인스턴스 환경에서도 안전한 일일 호출 제한 구현
- RAG 파이프라인 4단계 전 과정 DB 저장 → 답변 재현·프롬프트 품질 개선 기반 마련
- Parser 파싱 실패를 내부에서 흡수하고 DOCUMENT_SEARCH fallback 제공 → UX 안정성 확보