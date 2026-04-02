# 게시글 목록/검색/정렬 확장

## 목적
- 게시글 목록에서 검색, 정렬, 통계(좋아요/댓글 수) 제공.
- 일반 사용자 목록에서는 삭제 게시글 비노출.

## 주요 변경
- 게시글 목록 조회에 `keyword` 검색 파라미터 추가(제목/내용 대상).
- 정렬 타입에 최신순/좋아요순 적용.
- 응답에 `likeCount`, `commentCount`, `deleted` 포함.
- 일반 목록 조회는 `deleted=false`만 반환.

## 관련 API
- `GET /api/boards`
  - 주요 파라미터: `boardType`, `keyword`, `sortBy`, `page`, `size`
  - 정렬: `LATEST`, `LIKES`

## 구현 포인트
- Query Adapter에서 좋아요/댓글 수 집계와 정렬 조건을 함께 처리.
- 댓글 수는 삭제되지 않은 댓글 기준으로 계산.

## 관련 파일
- `src/main/kotlin/com/hirelog/api/board/presentation/controller/BoardController.kt`
- `src/main/kotlin/com/hirelog/api/board/application/BoardReadService.kt`
- `src/main/kotlin/com/hirelog/api/board/application/port/BoardQuery.kt`
- `src/main/kotlin/com/hirelog/api/board/infrastructure/adapter/BoardJpaQueryAdapter.kt`
- `src/main/kotlin/com/hirelog/api/board/presentation/controller/dto/response/BoardRes.kt`
