# 신고 기능 (Report)

**작업일**: 2026-04-02

---

## 배경

JobSummary, JobSummaryReview, Member 3종에 대한 신고 기능 필요.
어드민이 신고 목록을 조회하고 처리(RESOLVE/REJECT)할 수 있어야 함.

---

## 핵심 설계 결정

### nullable FK 3컬럼 방식 채택

`targetType(enum) + targetId(Long)` polymorphic 방식 거절.

**이유:**
- polymorphic 방식은 DB 레벨 FK constraint 불가
- cascade delete 불가, 고아 레코드 발생 가능
- nullable FK 3컬럼은 각 컬럼별 독립 FK 적용 가능

```kotlin
val jobSummaryId: Long?         // JOB_SUMMARY 신고 시만 non-null
val jobSummaryReviewId: Long?   // JOB_SUMMARY_REVIEW 신고 시만 non-null
val reportedMemberId: Long?     // MEMBER 신고 시만 non-null

// DB 컬럼 없음 — computed property
val targetType: ReportTargetType get() = when { ... }
```

### 중복 신고 방지

nullable 컬럼에는 일반 `UNIQUE` constraint 불가 (NULL ≠ NULL).
→ **PostgreSQL partial unique index** 사용:

```sql
CREATE UNIQUE INDEX uq_report_reporter_job_summary
    ON report (reporter_id, job_summary_id) WHERE job_summary_id IS NOT NULL;
-- 나머지 2개도 동일 패턴
```

Application-level에서도 `ReportWriteService.checkDuplicate()`로 이중 검증.

---

## ReportStatus 라이프사이클

```
PENDING → REVIEWED → RESOLVED
                   → REJECTED
```

- `review()`:  PENDING  → REVIEWED  (어드민 검토 시작)
- `resolve()`: REVIEWED → RESOLVED  (처리 완료)
- `reject()`:  REVIEWED → REJECTED  (반려)

---

## 생성 파일

| 파일 | 역할 |
|---|---|
| `report/domain/model/Report.kt` | 도메인 엔티티 |
| `report/domain/type/ReportTargetType.kt` | JOB_SUMMARY / JOB_SUMMARY_REVIEW / MEMBER |
| `report/domain/type/ReportReason.kt` | SPAM / INAPPROPRIATE / FALSE_INFO / COPYRIGHT / OTHER |
| `report/domain/type/ReportStatus.kt` | PENDING / REVIEWED / RESOLVED / REJECTED |
| `report/application/port/ReportCommand.kt` | 쓰기 포트 |
| `report/application/port/ReportQuery.kt` | 조회 포트 |
| `report/application/view/ReportView.kt` | QueryDSL 프로젝션 뷰 |
| `report/application/ReportWriteService.kt` | 신고 생성 / 상태 전이 |
| `report/application/ReportReadService.kt` | 신고 목록 조회 |
| `report/infrastructure/ReportJpaRepository.kt` | Spring Data JPA |
| `report/infrastructure/adapter/ReportJpaCommandAdapter.kt` | Command 포트 구현 |
| `report/infrastructure/adapter/ReportJpaQueryAdapter.kt` | Query 포트 구현 (QueryDSL) |
| `report/presentation/controller/dto/request/ReportWriteReq.kt` | 신고 요청 DTO |
| `report/presentation/controller/dto/response/ReportRes.kt` | 신고 응답 DTO |
| `report/presentation/controller/ReportController.kt` | REST Controller |

Flyway: `V5__alter_job_summary_review_columns.sql` 에 DDL 추가 (report 테이블, 인덱스, partial unique index)

---

## API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/reports` | 인증 사용자 | 신고 접수 |
| GET | `/api/reports` | ADMIN | 신고 목록 (status, targetType, page, size 필터) |
| PATCH | `/api/reports/{id}/review` | ADMIN | PENDING → REVIEWED |
| PATCH | `/api/reports/{id}/resolve` | ADMIN | REVIEWED → RESOLVED |
| PATCH | `/api/reports/{id}/reject` | ADMIN | REVIEWED → REJECTED |

---

## QueryAdapter 조인 구조

member 테이블을 2번 조인하므로 QDsl alias 필수.

```kotlin
private val reporter = QMember("reporter")           // 신고자
private val reportedMember = QMember("reportedMember") // 피신고 멤버

// LEFT JOIN 3개
.leftJoin(reporter).on(report.reporterId.eq(reporter.id))
.leftJoin(jobSummary).on(report.jobSummaryId.eq(jobSummary.id))
.leftJoin(reportedMember).on(report.reportedMemberId.eq(reportedMember.id))
```

`ReportView`에서 타겟별 식별 정보:
- `brandPositionName` — JOB_SUMMARY 대상일 때 non-null
- `reportedMemberUsername` — MEMBER 대상일 때 non-null
- `ReportRes.targetLabel`로 두 필드를 하나로 합쳐 프론트에 전달

---

## 신규 신고 대상 추가 시 수정 위치

1. `Report.kt` — nullable FK 컬럼 추가, `targetType` computed property 분기 추가
2. `ReportCommand.kt` — `existsByReporterAnd{NewTarget}()` 추가
3. `ReportJpaRepository.kt` — Spring Data 쿼리 메서드 추가
4. `ReportJpaCommandAdapter.kt` — 포트 구현 추가
5. `ReportWriteService.kt` — `checkDuplicate()` when 분기 추가, `report()` when 분기 추가
6. `ReportJpaQueryAdapter.kt` — LEFT JOIN 추가, `targetType` 필터 when 분기 추가
7. `ReportView.kt` — 새 대상 식별 필드 추가
8. `V5` 또는 신규 Flyway — FK 컬럼 + partial unique index 추가