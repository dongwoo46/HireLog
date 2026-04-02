# 채용 단계 합격/불합격 결과 UI

**작업일**: 2026-03-30
**페이지**: JobSummaryDetailPage — 준비 기록(prep) 탭

---

## 변경 내용

### 1. 결과 선택 버튼 (textarea 위)
- 합격 / 불합격 / 대기중 토글 버튼
- 다시 누르면 선택 해제 (null)
- 색상: 합격=초록(emerald), 불합격=빨강(red), 대기중=회색

### 2. 단계 사이드바 결과 뱃지
- 각 단계 버튼 오른쪽에 결과 뱃지 표시
- 결과 없으면 뱃지 미표시

---

## 수정 파일

| 파일 | 변경 |
|---|---|
| `src/types/jobSummary.ts` | `HiringStageResult` union type 추가, `HIRING_STAGE_RESULT_LABELS` Record 추가, `HiringStageView.result` 타입을 `string`에서 `HiringStageResult`로 변경 |
| `src/services/jdSummaryService.ts` | `saveStageNote(jobSummaryId, stage, note, result?)` — result 파라미터 추가, PATCH·POST 모두 전달 |
| `src/pages/JobSummaryDetailPage.tsx` | `stageResult` state 추가, 단계 전환·데이터 로드 시 result 동기화, 결과 토글 버튼 UI, 사이드바 뱃지 |

---

## 상태 흐름

```
loadPreparationData()
  → stages[activeStage].result → setStageResult()

activeStage 변경
  → stages[activeStage].result → setStageResult()

사용자가 버튼 클릭
  → setStageResult(r) or setStageResult(null) (토글)

savePreparation()
  → saveStageNote(id, stage, note, stageResult)
  → PATCH (성공) or POST (fallback)
```
