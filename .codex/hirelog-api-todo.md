# HireLog API TODO

기준일: 2026-03-30
대상: `hirelog-api`

## 이번 주 완료
- [x] `MemberJobSummary` 저장/해제 상태 전이 보정
- [x] `UNSAVED -> SAVED` 복구 흐름 명시화 (`restoreFromArchived`)
- [x] `save-type` 변경 시 `MemberJobSummary` 미존재 케이스 처리
- [x] 검색 응답 `isSaved` 계산 버그 수정 (미존재 상태 false 보장)
- [x] `MemberJobSummaryWriteServiceTest` 인코딩/DisplayName 정리

## 진행 중
- [ ] `changeSaveType(APPLY)` 정책 최종 확정
- [ ] 준비기록(stage) 작성 시점의 `APPLY` 전환 규칙 테스트 보강
- [ ] 예외 응답 메시지 일관성 정리 (`IllegalArgumentException` 중심)

## 다음 작업
- [ ] `relation` 패키지 테스트 전반 DisplayName/인코딩 일괄 정리
- [ ] `MemberJobSummary` 상태 전이 시나리오 통합 테스트 추가
- [ ] OpenAPI 문서에 save/apply/unsave 상태 전이 규칙 명시

## 메모
- 목록 저장 버튼은 `SAVED`만 처리
- `APPLY`는 준비기록 작성 시점에만 전환하는 정책으로 정리 중
