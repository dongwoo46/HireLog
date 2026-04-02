# 관리자 게시판/신고 관리

## 목적
- 관리자 페이지에서 게시글 전체(삭제 포함) 조회 및 상태 확인 지원.
- 신고 처리 시 대상(게시글/댓글) 즉시 삭제까지 가능한 운영 플로우 제공.

## 게시글 관리자 조회
- 관리자 전용 목록 API 추가.
- 삭제 여부 필터(`deleted`)와 페이지네이션 지원.
- 일반 사용자 목록과 분리해, 운영 화면에서 전체 상태 점검 가능.

## 신고 처리 강화
- 신고 상태 전이 외에 `처리 + 대상 삭제` 액션 제공.
- 게시글 신고/댓글 신고 모두 같은 엔드포인트 플로우에서 처리.

## 관련 API
- `GET /api/admin/boards`
  - 파라미터: `boardType`, `keyword`, `sortBy`, `deleted`, `page`, `size`
- `PATCH /api/reports/{id}/resolve-delete`

## 관련 파일
- `src/main/kotlin/com/hirelog/api/board/presentation/controller/BoardAdminController.kt`
- `src/main/kotlin/com/hirelog/api/report/presentation/controller/ReportController.kt`
- `src/main/kotlin/com/hirelog/api/report/application/ReportWriteService.kt`
