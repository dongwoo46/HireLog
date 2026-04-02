# hirelog-api 아키텍처 규칙

## 헥사고날 + CQRS 레이어 구조

```
{domain}/
├── domain/
│   ├── model/          ← JPA 엔티티 (Aggregate Root)
│   └── type/           ← enum 타입
├── application/
│   ├── port/
│   │   ├── {Entity}Command.kt   ← 쓰기 포트 (인터페이스)
│   │   └── {Entity}Query.kt     ← 조회 포트 (인터페이스)
│   ├── view/           ← QueryDSL Projection DTO
│   ├── {Entity}WriteService.kt  ← Command 유스케이스
│   └── {Entity}ReadService.kt   ← Query 유스케이스
├── infrastructure/
│   ├── {Entity}JpaRepository.kt ← Spring Data JPA
│   └── adapter/
│       ├── {Entity}JpaCommandAdapter.kt  ← Command 포트 구현체
│       └── {Entity}JpaQueryAdapter.kt    ← Query 포트 구현체 (QueryDSL)
└── presentation/
    └── controller/
        ├── dto/
        │   ├── request/
        │   └── response/
        └── {Entity}Controller.kt
```

---

## 포트 네이밍 규칙

| 종류 | 인터페이스 | 구현체 |
|---|---|---|
| 쓰기 포트 | `{Entity}Command` | `{Entity}JpaCommandAdapter` |
| 조회 포트 | `{Entity}Query` | `{Entity}JpaQueryAdapter` |

- `Command` 포트: `save()`, `findById()`, `existsBy...()` 등 단건 CUD
- `Query` 포트: View 반환, 페이징, 집계 조회 — `PagedResult<{Entity}View>` 반환

---

## 패키지 예외

- `relation/domain/model/` — 연관 관계 엔티티 (Like, Follow 등) 위치
  - 포트/서비스는 주체 도메인 패키지에 위치
  - 예: `BoardLike` → `relation/domain/model/`, 포트는 `board/application/port/`

---

## 트랜잭션 정책

| 레이어 | 정책 |
|---|---|
| `WriteService` | `@Transactional` |
| `ReadService` | `@Transactional(readOnly = true)` |
| `NotificationWriteService` | `@Transactional(propagation = REQUIRES_NEW)` — 핵심 트랜잭션과 분리 |
| `JobSummaryRequestWriteService.completeRequest/failRequest` | `@Transactional(propagation = REQUIRES_NEW)` — AFTER_COMMIT 리스너에서 호출 |

---

## 이벤트 기반 후처리 패턴

핵심 트랜잭션 커밋 후 부가 작업(알림, SSE)은 Spring Application Event로 처리.

```
@Transactional 비즈니스 로직
  → eventPublisher.publishEvent(...)  ← 트랜잭션 내에서 발행

    커밋 완료
      ↓
@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
  → 알림 저장 (REQUIRES_NEW)
  → SSE 전송
```

- `fallbackExecution = true`: 트랜잭션 없이 발행된 경우도 즉시 실행
- 리스너 실패가 핵심 비즈니스에 영향 없음 (try-catch 처리)
- 적용: `JobSummaryLifecycleListener` (Completed/Failed)

---

## QueryDSL 어댑터 패턴

```kotlin
// 동일 테이블 2번 조인 시 alias 필수
private val reporter = QMember("reporter")
private val reportedMember = QMember("reportedMember")

// View는 QueryDSL Projections.constructor용 data class
// computed property로 파생 필드 제공 (targetType, targetId 등)
```

- `PagedResult.of(items, page, size, totalElements)` 사용
- count 쿼리 분리: `select(entity.id.count()).from(...).fetchOne()`
- page/size 유효성: `require(page >= 0)`, `require(size in 1..100)`

---

## Flyway 마이그레이션

- 버전: `V{N}__{description}.sql`
- 개발 중 미적용 버전은 기존 파일에 append 가능
- 운영 적용 후에는 신규 버전으로 분리 (체크섬 불일치 방지)
- partial unique index는 반드시 Flyway로 관리 (JPA annotation 지원 불가)
