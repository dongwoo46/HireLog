# 채용 단계 합격/불합격 결과 저장

**작업일**: 2026-03-30

---

## 배경

준비 기록(HiringStageRecord)에 `result` 필드(PASSED/FAILED/PENDING)가 DB·도메인에
이미 존재했으나, POST `/stages` (AddStageReq)에 필드가 없어 신규 추가 시 저장 불가.
PATCH는 이미 result 저장 가능했음.

---

## 수정 파일

| 파일 | 변경 |
|---|---|
| `relation/presentation/controller/dto/MemberJobSummaryDto.kt` | `AddStageReq`에 `result: HiringStageResult? = null` 추가 |
| `relation/presentation/controller/MemberJobSummaryController.kt` | `addStage` 호출에 `result = request.result` 전달 |
| `relation/application/memberjobsummary/MemberJobSummaryWriteService.kt` | `addStage`에 `result: HiringStageResult? = null` 파라미터 추가 |
| `relation/domain/model/MemberJobSummary.kt` | `addStageRecord`에 `result: HiringStageResult? = null` 파라미터 추가, `HiringStageRecord` 생성 시 전달 |

---

## 기존 코드 (참고)

- `HiringStageResult`: `PASSED`, `FAILED`, `PENDING` (기존 enum, 변경 없음)
- `UpdateStageReq` (PATCH): 이미 `result` 있었음 — 변경 없음
- `HiringStageView`: 이미 `result: HiringStageResult?` 포함 — 변경 없음
