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
