# JdSummaryProcessing 생성 시점 변경

## 배경

`JdSummaryProcessing` 엔티티가 `JdSummaryGenerationFacade.execute()` (LLM 파이프라인 진입 시점)에서
생성됐기 때문에, **Python 전처리 실패** 경로에서 processing 엔티티가 존재하지 않았음.

```
Python 전처리 실패
  → JdPreprocessFailHandler.markFailed(processingId)
  → query.findById() → null (엔티티 없음)
  → Failed 이벤트 brandName=null, positionName=null
  → 알림 title "채용공고 분석 실패" (어느 공고인지 모름)
```

## 변경: 생성 시점을 JdIntakeService로 이동

### 이전 흐름

```
JdIntakeService → createRequest() + appendOutbox()
                     (startProcessing 없음)

JdSummaryGenerationFacade.execute()
  → startProcessing(requestId, brandName, positionName)  ← 여기서 생성
  → PRE_LLM → LLM → POST_LLM
```

### 변경 후 흐름

```
JdIntakeService
  → createRequest()
  → startProcessing(requestId, brandName, positionName)  ← intake 시점으로 이동
  → appendOutbox()

JdSummaryGenerationFacade.execute()
  → processingQuery.findById(requestId)  ← 기존 엔티티 조회
  → PRE_LLM → LLM → POST_LLM
```

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `JdSummaryProcessing.kt` | `create(id, brandName, positionName)` — 생성 시점에 brand/position 저장 |
| `JdSummaryProcessingWriteService.kt` | `startProcessing(requestId, brandName, positionName)` 파라미터 추가 |
| `JdIntakeService.kt` | `processingWriteService` 의존성 추가, 3개 메서드(`requestText/Ocr/Url`)에 `startProcessing` 호출 추가 |
| `JdSummaryGenerationFacade.kt` | `startProcessing` 제거 → `processingQuery.findById()` 로 교체, `JdSummaryProcessingQuery` 주입 추가 |
| 테스트 2개 | mock 시그니처 업데이트 |

## commandBrandName/commandPositionName 필드 의미

| 저장 시점 | 저장 값 | 용도 |
|---|---|---|
| `startProcessing` (intake) | 사용자 원본 입력값 | 전처리 실패 알림 title |
| `saveLlmResult` (LLM 이후) | 동일값으로 덮어씀 | StuckProcessing 복구 시 BrandPosition 생성 |

`saveLlmResult`에서도 동일 값을 저장하므로 이중 저장이지만 무해함.
완료 시(`markCompleted`) 두 필드 모두 null 처리됨.

## 실패 경로별 brandName/positionName 가용 여부

| 실패 경로 | 핸들러 | 가용 여부 |
|---|---|---|
| Python 전처리 실패 | `JdPreprocessFailHandler` | ✅ (intake에서 저장) |
| PRE_LLM 실패 | `PipelineErrorHandler.handle()` | ✅ (intake에서 저장) |
| LLM 실패 | `PipelineErrorHandler.handle()` | ✅ (intake에서 저장) |
| POST_LLM 실패 | `PipelineErrorHandler.handlePostLlm()` | ✅ (saveLlmResult 이후이므로 원래부터 있음) |
| Stuck 복구 실패 | `StuckProcessingRecoveryScheduler` | ✅ (llmResultJson 있는 상태이므로 원래부터 있음) |