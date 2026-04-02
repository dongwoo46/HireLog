# 리뷰 필드 분리 및 UI 동기화

## 목적
- 리뷰 입력/표시를 `장점(pros)`, `단점(cons)`, `면접 팁(tip)`으로 분리.
- 프론트/백엔드 스펙 불일치로 발생한 리뷰 등록 오류(500) 정합성 개선.

## 백엔드 기준
- `JobSummaryReview` 엔티티에서 `prosComment`, `consComment`, `tip` 필드 사용.
- 리뷰 생성/수정 로직이 위 3개 필드를 기준으로 검증 및 저장.

## 프론트 동기화 포인트
- 리뷰 작성 DTO/요청 바디를 분리 필드 구조에 맞춤.
- 리뷰 카드/상세 표시도 장점/단점/팁이 분리되어 보이도록 반영.
- 리뷰 정렬/페이지네이션/좋아요 토글 API 스펙과 화면 동기화.

## 관련 API
- `GET /api/job-summary/review/admin`
- `GET /api/job-summary/review/{reviewId}/like`
- `POST /api/job-summary/review/{reviewId}/like`
- `DELETE /api/job-summary/review/{reviewId}/like`

## 관련 파일
- `hirelog-api/src/main/kotlin/com/hirelog/api/job/domain/model/JobSummaryReview.kt`
- `hirelog-web/src/pages/JobSummaryDetailPage.tsx`
- `hirelog-web/src/components/admin/AdminReviewTab.tsx`
- `hirelog-web/src/services/jdSummaryService.ts`
- `hirelog-web/src/services/adminService.ts`
