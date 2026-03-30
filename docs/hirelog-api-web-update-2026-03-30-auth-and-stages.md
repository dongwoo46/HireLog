# HireLog API/Web 작업 정리 (2026-03-30)

대상: `hirelog-api`, `hirelog-web`

## 배경
- JD 상세의 준비기록 저장 시 같은 액션에서 `500`과 `400`이 연속 발생
- JD 목록/상세 조회를 비로그인 사용자도 가능하게 열어야 함

## 이슈 원인
1. `500` 원인
- 프론트가 stage 존재 여부와 무관하게 먼저 `PATCH /api/member-job-summary/{id}/stages` 호출
- 아직 stage가 없으면 서버 도메인에서 `IllegalStateException` 발생

2. `400` 원인
- `PATCH` 실패 후 프론트가 바로 `POST /stages` 재시도
- 이때 saveType이 `APPLY`가 아니면 도메인 규칙(`APPLY` 상태에서만 stage 관리 가능)에 걸려 `400` 발생

정리:
- `PATCH(없는 stage) -> 500`
- `catch 후 POST(saveType != APPLY) -> 400`

## 적용한 수정

### 1) JD 목록/상세 조회 인증 완화 (`hirelog-api`)
- `GET /api/job-summary/search` 공개
- `GET /api/job-summary/{id}` 공개
- 비로그인 호출을 처리하도록 `@CurrentUser`를 nullable로 변경하고 조회 로직에서 `memberId == null` 케이스 처리

변경 파일:
- `hirelog-api/src/main/kotlin/com/hirelog/api/common/config/security/SecurityConfig.kt`
- `hirelog-api/src/main/kotlin/com/hirelog/api/job/presentation/controller/JobSummaryController.kt`
- `hirelog-api/src/main/kotlin/com/hirelog/api/job/application/summary/JobSummaryReadService.kt`
- `hirelog-api/src/main/kotlin/com/hirelog/api/job/application/summary/port/JobSummaryQuery.kt`
- `hirelog-api/src/main/kotlin/com/hirelog/api/job/infra/persistence/jpa/adapter/JobSummaryJpaQueryAdapter.kt`

### 2) 준비기록 저장 흐름 보정 (`hirelog-web`)
- 기존: `PATCH` 실패 시 무조건 `POST` fallback
- 변경:
1. saveType이 `APPLY`가 아니면 먼저 `PATCH /save-type`으로 `APPLY` 전환
2. `GET /stages`로 stage 존재 확인
3. 존재하면 `PATCH`, 없으면 `POST`

추가:
- 상세 페이지 저장 성공 후 로컬 `memberSaveType`도 `APPLY`로 동기화

변경 파일:
- `hirelog-web/src/services/jdSummaryService.ts`
- `hirelog-web/src/pages/JobSummaryDetailPage.tsx`

## 기대 효과
- 준비기록 저장 시 `500 -> 400` 연쇄 에러 제거
- 비로그인 사용자도 JD 목록/상세 조회 가능
- stage 저장 로직이 도메인 제약(`APPLY`)과 정합되게 동작

## 검증
- `hirelog-api`: `./gradlew.bat compileKotlin` 통과
- `hirelog-api`: `./gradlew.bat compileTestKotlin` 통과
- `hirelog-web`: 프로젝트 전체 기존 lint 이슈 다수로 `npm run lint`는 실패(이번 변경 외 기존 오류 포함)
