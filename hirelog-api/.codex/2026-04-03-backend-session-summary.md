# 2026-04-03 Backend Session Summary

## 핵심 결론
- `job_snapshot_id` 충돌은 **snapshot 중복 생성 문제**가 아니라, 같은 snapshot에 대한 **job_summary 저장 경합** 문제.
- 따라서 post-llm 저장 충돌은 `FAILED`가 아니라 `DUPLICATE`로 종료하도록 처리.

## 이번에 반영된 변경

### 1) Post-LLM 충돌을 DUPLICATE로 전환
- 파일: `src/main/kotlin/com/hirelog/api/job/application/summary/JobSummaryWriteService.kt`
  - `job_summary` 저장 시 `DataIntegrityViolationException` 발생하고 같은 `snapshotId` summary가 이미 존재하면
  - `SnapshotAlreadySummarizedException`을 발생시키도록 변경.

- 파일: `src/main/kotlin/com/hirelog/api/job/application/summary/SnapshotAlreadySummarizedException.kt` (신규)
  - snapshot 기반 summary 중복 상황을 명시하는 런타임 예외 추가.

- 파일: `src/main/kotlin/com/hirelog/api/job/application/summary/pipeline/PipelineErrorHandler.kt`
  - `handlePostLlm()`에서 `SnapshotAlreadySummarizedException`을 별도 분기 처리:
  - `markDuplicate("SNAPSHOT_SUMMARY_DUPLICATE")` 호출
  - `JobSummaryRequestEvent.Duplicate` 발행
  - 기존 `POST_LLM_FAILED` 경로로 가지 않도록 조정.

- 파일: `src/main/kotlin/com/hirelog/api/job/application/jobsummaryprocessing/JdSummaryProcessingWriteService.kt`
  - `markDuplicate()` 반환 타입을 `JdSummaryProcessing`으로 변경.

- 파일: `src/main/kotlin/com/hirelog/api/job/domain/model/JdSummaryProcessing.kt`
  - 상태 전이 허용 범위 확장:
  - 기존 `RECEIVED -> DUPLICATE`만 허용하던 것을
  - `SUMMARIZING -> DUPLICATE`도 허용.

### 2) Pre-LLM skip 로그 개선
- 파일: `src/main/kotlin/com/hirelog/api/job/application/summary/JdSummaryGenerationFacade.kt`
  - 기존 로그: `[PIPELINE_PRE_LLM_SKIPPED]`
  - 변경 로그: `[PIPELINE_PRE_LLM_TERMINATED]`
  - `status`, `errorCode`, `duplicateReason`를 함께 기록하도록 변경.

### 3) SSE 연결 초기 전송 안정화
- 파일: `src/main/kotlin/com/hirelog/api/common/application/sse/SseEmitterManager.kt`
  - `subscribe()` 직후 초기 connect 이벤트 전송을 try-catch로 감싸서
  - 즉시 끊긴 연결에서 예외 전파/노이즈를 줄이도록 처리.

## 확인한 사실
- `job_snapshot`는 `canonical_hash` 유니크 인덱스가 있어 완전 동일 snapshot 중복 생성은 DB 레벨에서 방지됨.
- 이번 장애 로그(`uk_job_summary_snapshot_id`, key `(job_snapshot_id)=(3)`)는 동일 snapshot에 대한 summary insert 중복 시도.

## 현재 상태
- `./gradlew compileKotlin` 성공 확인.
- SSE 예외 처리 반영 완료:
  - `AsyncRequestTimeoutException` -> 204
  - `IOException` -> `/api/sse/*` 경로만 204, 그 외는 기존 500 처리
