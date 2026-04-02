# 2026-04-03 Backend Session Summary

## Scope
- General auth flow stabilization (signup/login/password reset with JWT setup alignment).
- JD summary pipeline reliability fixes (duplicate handling, recovery flow, status transitions).
- SSE and exception handling hardening.
- Outbox indexing robustness for missing Kafka headers.

## Root Cause Clarifications
- `uk_job_summary_snapshot_id` collisions were not snapshot-creation duplicates.
- They were concurrent/retry writes to `job_summary` for the same existing `job_snapshot_id`.
- SSE timeout/disconnect logs included normal long-poll lifecycle noise and needed bounded handling.

## Implemented Changes

### 1) Post-LLM duplicate collision path
- Added `SnapshotAlreadySummarizedException`.
- In `JobSummaryWriteService`, when `job_summary` insert hits duplicate snapshot constraint, throw dedicated duplicate exception.
- In `PipelineErrorHandler.handlePostLlm`, map that exception to duplicate path:
  - mark processing as duplicate (`SNAPSHOT_SUMMARY_DUPLICATE`)
  - publish `JobSummaryRequestEvent.Duplicate`
  - avoid `POST_LLM_FAILED` for this case.

### 2) Processing state transition safety
- Allowed `SUMMARIZING -> DUPLICATE` transition in `JdSummaryProcessing`.
- `markDuplicate()` now returns updated entity in write service for downstream event payload usage.
- Recovery scheduler guard:
  - do not force `POST_LLM_FAILED -> FAILED` through `markFailed()`.
  - only apply `markFailed()` where domain transition is valid.

### 3) Pre-LLM skip observability
- Improved pre-LLM skip log to include status/error context:
  - from generic skip log to terminated log with `status`, `errorCode`, `duplicateReason`.

### 4) SSE stability
- Wrapped initial SSE connect event send in `SseEmitterManager.subscribe()` with safe cleanup on immediate disconnect.
- Global exception handling updated:
  - `AsyncRequestTimeoutException` -> `204`
  - `IOException` for `/api/sse/*` -> `204`
  - non-SSE IO exceptions keep normal error path.

### 5) Log signal cleanup
- Kept first-cause logs as primary signal.
- Reduced downstream propagation noise by lowering some repeated error logs to warn/info.

### 6) Outbox indexing header robustness
- In `JobSummaryIndexingConsumer`, event type extraction now checks multiple header names:
  - `eventType`, `event_type`, `type`
- If missing, infer from payload shape (`{id}` => delete) to avoid wrong behavior.
- Updated CDC scripts to place `event_type` into Kafka header `eventType`:
  - `transforms.outbox.table.fields.additional.placement=event_type:header:eventType`

## Runtime Verification
- Kafka Connect runtime config checked:
  - connector is `RUNNING`
  - `event_type` mapping and additional header placement applied.
- Build verification:
  - `./gradlew compileKotlin` passed after changes.

## Notes
- Some warning logs can still appear while consuming older records produced before header mapping fix.
- Current behavior is safe due to consumer-side fallback and duplicate-safe post-llm handling.
