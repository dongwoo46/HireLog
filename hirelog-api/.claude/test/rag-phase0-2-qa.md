# RAG Phase 0~2 수동 QA 체크리스트

Phase 3~4 (Executor, Composer, RagService, RagController) 미구현 상태 기준.
직접 호출 가능한 항목만 수록.

---

## 신규 Admin API 목록 (이번 작업에서 추가됨)

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/admin/job-summary/direct` | JD 텍스트로 직접 Gemini 요약 생성 |
| POST | `/api/admin/job-summary/reindex-all` | 전체 재인덱싱 (인덱스 삭제 + 재생성 + 재인덱싱) |
| POST | `/api/admin/job-summary/reindex-embedding` | 임베딩 누락 문서만 재임베딩 |
| POST | `/api/admin/rag/parse` | LLM Parser 단독 테스트 (질문 → RagQuery 결과 확인) |

> 모두 `hasRole('ADMIN')` 필요

---

## Admin API — POST /api/admin/job-summary/direct

### A-1. 정상 요약 생성

```json
POST /api/admin/job-summary/direct
{
  "brandName": "토스",
  "positionName": "Backend Engineer",
  "jdText": "토스는 금융 서비스를 더 쉽게 만들기 위해... Kotlin, Spring Boot, Kafka 기반..."
}
```

**확인 항목:**
- [ ] 응답 `{ "summaryId": <number> }`
- [ ] DB `job_summary` 레코드 생성 확인
- [ ] `company_domain = 'FINTECH'`, `company_size = 'SCALE_UP'` (한글/null 아닌 영문 enum)
- [ ] `tech_stack`, `summary_text`, `responsibilities` 필드 채워짐

---

### A-2. 프롬프트 enum 출력 확인

```json
GET /api/admin/job-summary/prompt/system-instruction
```

**확인 항목:**
- [ ] 응답 systemInstruction에 `FINTECH`, `E_COMMERCE` 등 영문 enum 이름 포함
- [ ] 기존 한글 값("핀테크", "커머스") 없음
- [ ] `SEED`, `EARLY_STARTUP`, `GROWTH_STARTUP`, `SCALE_UP` 등 CompanySize 값 포함

---

## Admin API — POST /api/admin/job-summary/reindex-all

### B-1. 기본 호출

```
POST /api/admin/job-summary/reindex-all?batchSize=50
```

**확인 항목:**
- [ ] 응답 `{ "successCount": <number> }`
- [ ] successCount가 DB `job_summary` 레코드 수와 일치 (또는 근접)
- [ ] OpenSearch 인덱스가 재생성됨 (`GET /job-summary/_mapping` 으로 확인)
- [ ] `embeddingVector`가 `knn_vector` 타입 (768차원)
- [ ] `companyDomain`, `companySize`가 `keyword` 타입

---

### B-2. batchSize 유효성 검증

```
POST /api/admin/job-summary/reindex-all?batchSize=0
POST /api/admin/job-summary/reindex-all?batchSize=201
```

**확인 항목:**
- [ ] 두 케이스 모두 400 또는 예외 응답 (유효 범위: 1~200)

---

### B-3. 임베딩 서버 다운 상태에서 reindexAll

임베딩 서버 중단 후 호출.

**확인 항목:**
- [ ] 예외 전파 없이 완료
- [ ] `successCount` 반환 (임베딩 실패분 제외하고 계산되는지)
- [ ] OpenSearch 문서에 `embeddingVector = null`로 저장됨 (BM25는 동작)

---

## Admin API — POST /api/admin/job-summary/reindex-embedding

### C-1. 누락 문서만 재임베딩

reindex-all 후 일부 문서의 `embeddingVector`를 직접 null로 업데이트한 뒤:

```sql
UPDATE job_summary SET embedding_vector = NULL WHERE id IN (1, 2, 3);
```

```
POST /api/admin/job-summary/reindex-embedding?batchSize=100
```

**확인 항목:**
- [ ] 응답 `{ "successCount": 3 }` (null인 3건만 처리)
- [ ] OpenSearch에서 해당 문서 `embeddingVector` 채워짐 확인

---

### C-2. 누락 문서 없을 때

모든 문서에 embeddingVector가 존재할 때:

```
POST /api/admin/job-summary/reindex-embedding
```

**확인 항목:**
- [ ] 응답 `{ "successCount": 0 }`
- [ ] 불필요한 임베딩 서버 호출 없음

---

### C-3. batchSize 유효성 검증

```
POST /api/admin/job-summary/reindex-embedding?batchSize=0
POST /api/admin/job-summary/reindex-embedding?batchSize=501
```

**확인 항목:**
- [ ] 두 케이스 모두 400 또는 예외 응답 (유효 범위: 1~500)

---

## Phase 0 — companyDomain / companySize 필드

### 0-1. Flyway 마이그레이션 확인

DB에서 직접 확인.

```sql
-- 컬럼 존재 및 타입 확인
SELECT column_name, data_type, character_maximum_length, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'job_summary'
  AND column_name IN ('company_domain', 'company_size');
```

**기대값:**
| column_name | character_maximum_length | column_default | is_nullable |
|---|---|---|---|
| company_domain | 30 | OTHER | NO |
| company_size | 20 | UNKNOWN | NO |

---

### 0-2. 기존 데이터 DEFAULT 확인

마이그레이션 이후 기존 레코드가 DEFAULT로 채워졌는지 확인.

```sql
SELECT id, brand_name, company_domain, company_size
FROM job_summary
ORDER BY id
LIMIT 10;
```

**기대값:** company_domain = 'OTHER', company_size = 'UNKNOWN'

---

### 0-3. 신규 JD 요약 생성 — 핀테크 도메인

토스 등 명확한 핀테크 JD로 요약 생성 후 DB/API 응답 확인.

**Admin API 호출 (예시):**
```
POST /api/admin/job-summaries
{
  "brandName": "토스",
  "jdText": "토스는 금융 서비스를 더 쉽게 만들기 위해... 결제, 송금, 대출, 증권 서비스... Kotlin, Spring Boot, Kafka..."
}
```

**확인 항목:**
- [ ] `companyDomain = "FINTECH"` (OTHER 아님)
- [ ] `companySize = "SCALE_UP"` (토스는 유니콘급)
- [ ] DB `job_summary` 테이블에 저장 확인

---

### 0-4. 신규 JD 요약 생성 — 게임 도메인

```
brandName: "크래프톤"
jdText: "배틀그라운드, 게임 서버 개발, Unreal Engine..."
```

**확인 항목:**
- [ ] `companyDomain = "GAME"`
- [ ] `companySize = "LARGE_CORP"` (상장 대기업)

---

### 0-5. 신규 JD 요약 생성 — 외국계

```
brandName: "구글코리아"
jdText: "Google Korea, Software Engineer, distributed systems..."
```

**확인 항목:**
- [ ] `companyDomain = "AI_ML"` 또는 `"CLOUD_INFRA"`
- [ ] `companySize = "FOREIGN_CORP"`

---

### 0-6. 신규 JD 요약 생성 — 시드 스타트업 (규모 판단 어려운 케이스)

```
brandName: "신생회사ABC"
jdText: "창업 1년차 스타트업, 팀 5명, 시드 투자 유치..."
```

**확인 항목:**
- [ ] `companySize = "SEED"` 또는 `"EARLY_STARTUP"`
- [ ] `"UNKNOWN"` 나오면 프롬프트 개선 필요

---

### 0-7. 도메인 모호한 케이스 (OTHER fallback)

```
brandName: "ABC컨설팅"
jdText: "컨설팅 회사, 다양한 도메인 프로젝트... SI 업체..."
```

**확인 항목:**
- [ ] `companyDomain = "ENTERPRISE_SW"` 또는 `"OTHER"`
- [ ] 절대 null 또는 한글 값("핀테크" 등) 나오면 안 됨

---

### 0-8. OpenSearch 인덱싱 확인

요약 생성 후 Kafka Consumer → OpenSearch 인덱싱이 완료되면:

```
GET /job-summary/_search
{
  "query": { "term": { "companyDomain": "FINTECH" } },
  "_source": ["brandName", "companyDomain", "companySize"],
  "size": 3
}
```

**확인 항목:**
- [ ] `companyDomain`, `companySize` 필드가 인덱스에 존재
- [ ] keyword 타입으로 정확히 일치 검색 동작
- [ ] 값이 영문 enum 이름 (예: "FINTECH", "SCALE_UP")

---

### 0-9. reindexAll 후 기존 데이터 OpenSearch 확인

Admin reindexAll 실행 후:

```
GET /job-summary/_mapping
```

**확인 항목:**
- [ ] `companyDomain`, `companySize`가 `keyword` 타입으로 매핑
- [ ] `embeddingVector`가 `knn_vector` 타입 (768차원)

---

## Phase 1 — k-NN 검색 (searchByVector)

Phase 4 (RagController) 미구현 상태이므로 직접 API 호출 불가.
OpenSearch에 직접 쿼리로 동작 확인.

### 1-1. OpenSearch k-NN 직접 쿼리

임베딩 서버에서 쿼리 벡터를 먼저 얻은 뒤 테스트.

```bash
# 1) 임베딩 서버에서 벡터 추출
curl -X POST http://localhost:8001/embed/query \
  -H "Content-Type: application/json" \
  -d '{"text": "Kotlin Spring Boot 백엔드 개발자"}'
```

```
# 2) 반환된 벡터로 OpenSearch k-NN 쿼리
POST /job-summary/_search
{
  "size": 5,
  "query": {
    "knn": {
      "embeddingVector": {
        "vector": [/* 위에서 얻은 768차원 벡터 */],
        "k": 5
      }
    }
  },
  "_source": ["id", "brandName", "positionName", "companyDomain", "companySize"]
}
```

**확인 항목:**
- [ ] 결과 반환 (hits.hits 존재)
- [ ] `_score` 값이 0~1 사이 (cosinesimil)
- [ ] 질문과 의미적으로 유사한 JD가 상위에 오는지 (주관적 판단)

---

### 1-2. k-NN + companyDomain 필터 조합

```
POST /job-summary/_search
{
  "size": 5,
  "query": {
    "bool": {
      "must": {
        "knn": {
          "embeddingVector": { "vector": [...], "k": 5 }
        }
      },
      "filter": [
        { "term": { "companyDomain": "FINTECH" } }
      ]
    }
  }
}
```

**확인 항목:**
- [ ] 결과가 모두 `companyDomain = "FINTECH"`
- [ ] k-NN 결과가 필터로 좁혀지는지

---

## Admin API — POST /api/admin/rag/parse

### D-1. intent 분류 — DOCUMENT_SEARCH

```json
POST /api/admin/rag/parse
{ "question": "Kafka 쓰는 스타트업 백엔드 공고 찾아줘" }
```

**기대 응답:**
```json
{
  "intent": "DOCUMENT_SEARCH",
  "semanticRetrieval": true,
  "aggregation": false,
  "baseline": false,
  "parsedText": "Kafka 스타트업 백엔드",
  "filters": { "techStacks": ["Kafka"] }
}
```

---

### D-2. intent 분류 — PATTERN_ANALYSIS

```json
{ "question": "저장한 공고들 공통 기술스택 분석해줘" }
```

**기대 응답:**
- `intent = PATTERN_ANALYSIS`
- `semanticRetrieval = false`
- `aggregation = true`
- `filters.saveType = SAVED`

---

### D-3. intent 분류 — EXPERIENCE_ANALYSIS

```json
{ "question": "내가 1차 면접 불합격한 공고 패턴 알려줘" }
```

**기대 응답:**
- `intent = EXPERIENCE_ANALYSIS`
- `filters.stage = INTERVIEW_1`
- `filters.stageResult = FAILED`

---

### D-4. intent 분류 — STATISTICS

```json
{ "question": "지금까지 지원한 공고 몇 개야?" }
```

**기대 응답:**
- `intent = STATISTICS`
- `filters.saveType = APPLY`
- `semanticRetrieval = false`

---

### D-5. intent 분류 — SUMMARY

```json
{ "question": "토스 최근 공고 내용 요약해줘" }
```

**기대 응답:**
- `intent = SUMMARY`
- `semanticRetrieval = true`
- `filters.brandName = "토스"`

---

### D-6. 복합 필터 파싱

```json
{ "question": "2024년 이후 저장한 핀테크 공고 중 서류 합격한 것" }
```

**기대 응답:**
- `filters.saveType = SAVED`
- `filters.companyDomain = "FINTECH"` (또는 유사값)
- `filters.stage = DOCUMENT`
- `filters.stageResult = PASSED`
- `filters.dateRangeFrom = "2024-01-01"`

---

### D-7. KEYWORD_SEARCH fallback — 모호한 질문

```json
{ "question": "그냥 아무거나" }
```

**기대 응답:**
- `intent = KEYWORD_SEARCH`
- `semanticRetrieval = false`
- `parsedText = "그냥 아무거나"`

---

### D-8. KEYWORD_SEARCH fallback — Gemini 장애 시

Gemini API 키를 잘못된 값으로 교체 후 호출.

**기대 응답:**
- [ ] 예외 없이 200 응답
- [ ] `intent = KEYWORD_SEARCH`
- [ ] `parsedText = 원본 질문`
- [ ] 서버 로그에 `[RAG_PARSER_FAILED]` 출력

---

## 공통 확인 항목

- [ ] Gemini 응답에서 companyDomain이 영문 enum (대문자) 으로 오는지 — 한글 "핀테크" 오면 프롬프트 재조정 필요
- [ ] Assembler fallback: LLM이 없는 enum 값 반환 시 OTHER/UNKNOWN으로 저장되는지
- [ ] 로그 패턴 확인: `[RAG_PARSER_FAILED]` 로그가 파싱 실패 시 출력되는지