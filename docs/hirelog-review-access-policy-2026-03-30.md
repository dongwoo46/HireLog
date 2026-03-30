# HireLog 리뷰 조회 정책 변경 기록 (2026-03-30)

대상: `hirelog-api`, `hirelog-web`

## 변경 배경
- JD 상세 리뷰 영역에서 비로그인/권한 정책이 혼재되어 안내 문구가 중복 노출됨
- 리뷰 조회 정책을 명확히 정리할 필요가 있었음

## 최종 정책
1. 리뷰 조회는 로그인 사용자만 가능
2. 로그인 사용자 중에서도, 본인이 등록한 JD가 1건 이상 있을 때만 조회 가능
3. 비로그인 상태에서는 리뷰 탭에 안내 문구 1회만 노출

## 백엔드 변경
- `GET /api/job-summary/review/{jobSummaryId}`에 인증 강제
  - `@PreAuthorize("isAuthenticated()")` 적용
- 리뷰 조회 서비스에서 추가 정책 검증
  - `member_job_summary` 기준 `existsAnyByMemberId(memberId)` 체크
  - 실패 시 `IllegalArgumentException`으로 정책 메시지 반환
- 관련 포트/어댑터/Querydsl에 `existsAnyByMemberId` 추가

수정 파일:
- `hirelog-api/src/main/kotlin/com/hirelog/api/job/presentation/controller/JobSummaryReviewController.kt`
- `hirelog-api/src/main/kotlin/com/hirelog/api/job/application/summary/JobSummaryReviewReadService.kt`
- `hirelog-api/src/main/kotlin/com/hirelog/api/relation/application/memberjobsummary/port/MemberJobSummaryQuery.kt`
- `hirelog-api/src/main/kotlin/com/hirelog/api/relation/infra/persistence/jpa/adapter/MemberJobSummaryJpaQuery.kt`
- `hirelog-api/src/main/kotlin/com/hirelog/api/relation/infra/persistence/jpa/repository/MemberJobSummaryJpaQueryDsl.kt`

## 프론트 변경
- JD 상세 상단 비로그인 안내 배너 제거 (중복 노출 제거)
- 리뷰 탭 내부 안내만 유지
- 비로그인 상태에서는 리뷰 리스트 API 호출 자체를 차단

수정 파일:
- `hirelog-web/src/pages/JobSummaryDetailPage.tsx`

## 주석 보강
- 정책 의도가 드러나도록 리뷰 조회 컨트롤러/서비스/Querydsl에 주석 추가

## 검증
- API: `hirelog-api` `compileKotlin` 통과
- Web: `hirelog-web` `npm run build` 통과
