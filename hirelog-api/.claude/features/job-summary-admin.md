# JobSummary Admin 기능 정리

## 활성화 / 비활성화 → OpenSearch 연동

### 코드 위치

| 역할 | 파일 |
|------|------|
| activate/deactivate 비즈니스 로직 | `job/application/summary/JobSummaryWriteService.kt` L201~241 |
| API 엔드포인트 | `job/presentation/controller/JobSummaryController.kt` L151~170 |
| OpenSearch Consumer | `job/infra/kafka/consumer/JobSummaryIndexingConsumer.kt` |
| OpenSearch Adapter (index/delete) | `job/infra/persistence/opensearch/JobSummaryOpenSearchAdapter.kt` |

### 흐름

```
PATCH /api/job-summary/{id}/deactivate   (@PreAuthorize("hasRole('ADMIN')"))
  → JobSummaryWriteService.deactivate()
      → summary.deactivate()                          // isActive = false
      → summaryCommand.update(summary)               // DB 반영
      → OutboxEvent(eventType=DELETED, payload={"id":123})  // Outbox 저장
          → Debezium CDC → Kafka
              → JobSummaryIndexingConsumer.handleDelete()
                  → JobSummaryOpenSearchAdapter.delete(id)  // OpenSearch 문서 삭제

PATCH /api/job-summary/{id}/activate     (@PreAuthorize("hasRole('ADMIN')"))
  → JobSummaryWriteService.activate()
      → summary.activate()                            // isActive = true
      → summaryCommand.update(summary)               // DB 반영
      → OutboxEvent(eventType=CREATED, payload=전체 페이로드) // Outbox 저장
          → Debezium CDC → Kafka
              → JobSummaryIndexingConsumer.handleIndex()
                  → JobSummaryOpenSearchAdapter.index(payload) // OpenSearch 문서 재인덱싱
```

### 트러블슈팅 포인트

- Consumer에서 `eventType` 헤더가 없으면 CREATED로 fallback됨
  - Debezium Outbox EventRouter 설정에서 `eventType` 컬럼을 헤더로 매핑하는지 확인
  - 관련 로그: `[JOB_SUMMARY_INDEXING_HEADER_MISSING]`
- `payload`가 `{"id":N}` 형태인데 eventType이 CREATED로 오면 오분류 방어 로직 있음
  - `tree.size() == 1 && tree.has("id")` 체크로 삭제 이벤트 감지

---

## Admin 목록/상세 조회 API

### API

| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/admin/job-summary?isActive=&page=&size=` | 목록 조회 (isActive 생략 시 전체) |
| `GET` | `/api/admin/job-summary/{id}` | 상세 조회 (비활성화된 것도 조회 가능) |

### 코드 위치

| 역할 | 파일 |
|------|------|
| Controller | `job/presentation/controller/JobSummaryAdminController.kt` |
| ReadService | `job/application/summary/JobSummaryReadService.kt` (`searchAdmin`, `getDetailAdmin`) |
| Query 포트 | `job/application/summary/port/JobSummaryQuery.kt` (`searchAdmin`, `findDetailByIdAdmin`) |
| QueryAdapter | `job/infra/persistence/jpa/adapter/JobSummaryJpaQueryAdapter.kt` |
| QueryDSL 구현 | `job/infra/persistence/jpa/repository/JobSummaryJpaQueryDslRepositoryImpl.kt` (`searchAdmin`, `findDetailByIdAdmin`) |
| 목록 View | `job/application/summary/view/JobSummaryAdminView.kt` |
| 상세 View | `job/application/summary/view/JobSummaryDetailView.kt` (isActive 필드 포함) |

### 설계 포인트

- 일반 사용자 조회 (`findDetailById`): `WHERE isActive = true` 필터 적용
- Admin 조회 (`findDetailByIdAdmin`): `isActive` 필터 없음 → 비활성화된 것도 조회 가능
- `JobSummaryDetailView.isActive` 기본값 `true` → 기존 사용자 쪽 코드 영향 없음
- Admin 목록 (`searchAdmin`): `isActive = null` → 전체, `true`/`false` → 필터링
- 정렬: `createdAt DESC`

---

## Admin 직접 생성 API (Python 전처리 파이프라인 미사용)

### API

| Method | URL | 입력 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/admin/job-summary/direct` | `{brandName, positionName, jdText, sourceUrl?}` | 텍스트 직접 붙여넣기 |
| `POST` | `/api/admin/job-summary/direct-url` | `{brandName, positionName, url}` | URL 크롤링 → 텍스트/이미지 자동 분기 |
| `POST` | `/api/admin/job-summary/direct-images` | `{brandName, positionName, images, sourceUrl?}` | base64 이미지 직접 업로드 |

모든 엔드포인트: `@PreAuthorize("hasRole('ADMIN')")`

### 처리 흐름

```
POST /direct
  → JobSummaryAdminService.createDirectly()
      → buildAdminCanonicalMap(jdText)   // "etc" 키 단일 사용 (섹션 분리 없음)
      → 중복 체크 (sourceUrl + canonicalHash)
      → Gemini 동기 호출 (summarizeJobDescriptionAsync)
      → PostLlmProcessor.executeForAdmin()  // Snapshot + JobSummary + Outbox 단일 트랜잭션

POST /direct-url
  → JobSummaryAdminService.createFromUrl()
      → AdminJdUrlFetchPort.fetch(url)       // 임베딩 서버 /admin/fetch-url 동기 호출 (timeout 90s)
          ├─ SUCCESS    → createDirectly()   // 텍스트 경로
          ├─ IMAGE_BASED → createFromImages() // 이미지 경로 (Gemini 멀티모달)
          ├─ INSUFFICIENT → IllegalStateException
          └─ ERROR       → IllegalStateException

POST /direct-images
  → JobSummaryAdminService.createDirectlyFromImages()
      → 중복 체크 (sourceUrl이 있을 때만)
      → Gemini 멀티모달 동기 호출 (summarizeFromImagesAsync)
      → PostLlmProcessor.executeForAdmin()
```

### Python 임베딩 서버 연동 (`/admin/fetch-url`)

- 기존 임베딩 서버 FastAPI 프로세스에 추가 (`embedding/admin_router.py`)
- Spring의 `embeddingWebClient` 빈 재사용
- 플랫폼별 크롤링 전략:

| 플랫폼 | 크롤링 방식 |
|--------|------------|
| JOBKOREA | `UrlFetcher` (정적 HTTP) |
| SARAMIN | `SaraminPlaywrightFetcher` (Playwright, iframe 구조) |
| WANTED, REMEMBER, 기타 | `PlaywrightFetcher` |

- 텍스트 추출 후 `preprocess_url_text()` 적용 (노이즈 제거, 섹션 분리 없음)
- 텍스트 길이 < 200 → JD 이미지 추출 시도 → `IMAGE_BASED` 응답
- 응답 status: `SUCCESS` / `IMAGE_BASED` / `INSUFFICIENT` / `ERROR`

### 이미지 처리 (Gemini 멀티모달)

- OCR 미사용 — 이미지를 base64 그대로 Gemini에 전달
- `GeminiClient.generateContentWithImagesAsync()`: `inlineData` parts 구성, timeout 60s
- `JobSummaryLlm.summarizeFromImagesAsync()` 포트로 추상화

### canonicalMap 설계 (Admin 경로 전용)

- 텍스트 경로: `mapOf("etc" to lines)` — 섹션 분리 없이 전체 텍스트를 `ETC` 키 하나에
- 이미지 경로: `mapOf("etc" to emptyList())` — 해시 계산용 더미, 실제 내용은 이미지에서 추출
- `"etc"` 선택 이유: `JdSection.ETC`가 `toCanonicalText()`에서 iterate되는 유효한 키이며, raw 비분리 텍스트에 가장 적합한 의미

### 코드 위치

| 역할 | 파일 |
|------|------|
| Service | `job/application/summary/JobSummaryAdminService.kt` |
| URL 추출 포트 | `job/application/summary/port/AdminJdUrlFetchPort.kt` |
| URL 추출 어댑터 | `job/infra/external/admin/AdminJdUrlFetchAdapter.kt` |
| 이미지 LLM 포트 | `job/application/summary/port/JobSummaryLlm.kt` (`summarizeFromImagesAsync`) |
| Gemini 멀티모달 | `job/infra/external/gemini/GeminiClient.kt` (`generateContentWithImagesAsync`) |
| Gemini 이미지 프롬프트 | `job/infra/external/gemini/GeminiPromptBuilder.kt` (`buildJobSummaryFromImagesPrompt`) |
| Python 크롤링 엔드포인트 | `preprocess-pipeline/src/embedding/admin_router.py` |
| Controller | `job/presentation/controller/JobSummaryAdminController.kt` |
| 요청 DTO | `dto/request/JobSummaryAdminCreateReq.kt` |
| | `dto/request/JobSummaryAdminCreateFromUrlReq.kt` |
| | `dto/request/JobSummaryAdminCreateFromImagesReq.kt` |
