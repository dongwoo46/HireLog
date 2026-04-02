# 2026-04-02 Review Frontend Sync

## Scope
- JD 상세 리뷰 탭과 Admin 리뷰 탭을 백엔드 리뷰 스펙 변경에 맞게 동기화.
- 리뷰 필드 `prosComment`, `consComment`, `tip` 기준으로 프론트 타입/서비스/UI 업데이트.
- 리뷰 정렬/페이지네이션, 좋아요 토글 연동 반영.

## Backend Alignment
- 리뷰 조회 공개(GET) 허용 반영 상태 기준으로 프론트 조회 흐름 유지.
- Review API 정렬 파라미터 반영:
  - `LATEST`, `LIKES`, `RATING`, `DIFFICULTY`, `SATISFACTION`
- Admin 전체 리뷰 조회 API 사용:
  - `GET /api/job-summary/review/admin`
- 좋아요 API 연동:
  - `GET /api/job-summary/review/{reviewId}/like`
  - `POST /api/job-summary/review/{reviewId}/like`
  - `DELETE /api/job-summary/review/{reviewId}/like`

## Changed Files (Frontend)
- `hirelog-web/src/pages/JobSummaryDetailPage.tsx`
- `hirelog-web/src/components/admin/AdminReviewTab.tsx`
- `hirelog-web/src/services/jdSummaryService.ts`
- `hirelog-web/src/services/adminService.ts`
- `hirelog-web/src/types/jobSummary.ts`
- `hirelog-web/src/types/admin.ts`

## Changed Files (Backend side touched in this session)
- `hirelog-api/src/main/resources/db/migration/V5__alter_job_summary_review_columns.sql` (복구 및 통합 유지)

## Verification
- Frontend build: `npm run build` (pass)
- Backend compile: `./gradlew compileKotlin` (pass)
- 인코딩 점검:
  - 대상 수정 파일에서 `U+FFFD(�)` 없음
  - 사용자 노출 문자열 기준 `??` 깨짐 패턴 없음

