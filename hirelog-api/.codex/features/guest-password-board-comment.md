# 비로그인 작성자 비밀번호 인증

## 목적
- 비로그인 사용자가 작성한 게시글/댓글의 소유권을 비밀번호로 검증.
- 로그인 사용자는 기존처럼 비밀번호 없이 수정/삭제 가능.

## 동작 규칙
- 비로그인 작성 시 비밀번호 필수.
- 비로그인 수정/삭제 시 비밀번호 검증 성공 시에만 처리.
- 관리자(`ADMIN`)는 비밀번호 없이 관리 가능.

## 데이터 변경
- 게시글/댓글에 `guest_password_hash` 컬럼 추가.
- 비밀번호 원문 저장 없이 해시만 저장.

## 관련 API
- 게시글
  - `POST /api/boards` (body: `guestPassword`)
  - `PATCH /api/boards/{id}` (body: `guestPassword`)
  - `DELETE /api/boards/{id}?guestPassword=...`
- 댓글
  - `POST /api/boards/{boardId}/comments` (body: `guestPassword`)
  - `PATCH /api/boards/{boardId}/comments/{id}` (body: `guestPassword`)
  - `DELETE /api/boards/{boardId}/comments/{id}?guestPassword=...`

## 관련 파일
- `src/main/resources/db/migration/V8__add_guest_password_hash_to_board_comment.sql`
- `src/main/kotlin/com/hirelog/api/board/domain/Board.kt`
- `src/main/kotlin/com/hirelog/api/comment/domain/Comment.kt`
- `src/main/kotlin/com/hirelog/api/board/application/BoardWriteService.kt`
- `src/main/kotlin/com/hirelog/api/comment/application/CommentWriteService.kt`
- `src/main/kotlin/com/hirelog/api/board/presentation/controller/dto/request/BoardWriteReq.kt`
- `src/main/kotlin/com/hirelog/api/comment/presentation/controller/dto/request/CommentWriteReq.kt`
