# 실패 알림 title에 brandName/positionName 표시

## 배경

모든 실패 알림 title이 `"채용공고 분석 실패"`로 고정되어 있어
어떤 채용공고가 실패했는지 사용자가 알 수 없었음.

## 변경 내용

### 실패 알림 title 포맷
- brandName + positionName 있을 때: `"카카오 백엔드 개발자 분석 실패"`
- 없을 때 (폴백): `"채용공고 분석 실패"`

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `JobSummaryRequestEvent.kt` | `Failed`에 `brandName?`, `positionName?` 추가 |
| `JdSummaryProcessingWriteService.kt` | `markFailed()` → `JdSummaryProcessing?` 반환, `markPostLlmFailed()` → `JdSummaryProcessing` 반환 |
| `JdPreprocessFailHandler.kt` | 반환된 processing에서 brand/position 추출 후 Failed 이벤트에 전달 |
| `PipelineErrorHandler.kt` | `handle()`, `handlePostLlm()` 동일 처리 |
| `JobSummaryLifecycleListener.kt` | `onFailed()` title 조건부 구성 |
| `StuckProcessingRecoveryScheduler.kt` | `processing.commandBrandName/commandPositionName` 직접 사용 |

### title 구성 로직 (JobSummaryLifecycleListener)

```kotlin
title = if (!event.brandName.isNullOrBlank() && !event.positionName.isNullOrBlank())
    "${event.brandName} ${event.positionName} 분석 실패"
else
    "채용공고 분석 실패"
```

## 연관 이슈: commandBrandName/commandPositionName null 문제

`Failed` 이벤트에 brand/position을 담으려면 `JdSummaryProcessing` 엔티티에서 꺼내야 함.
그런데 `commandBrandName/commandPositionName`은 `saveLlmResult()` 시점(LLM 이후)에만 저장됨.

→ **PRE_LLM 실패, Python 전처리 실패** 경로에서는 null

### 해결: startProcessing 시점에 저장

`JdIntakeService`에서 `startProcessing(requestId, brandName, positionName)` 호출.
상세는 `features/jd-summary-processing-lifecycle.md` 참고.