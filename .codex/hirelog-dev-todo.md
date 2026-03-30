# HireLog Dev TODO

기준일: 2026-03-30
범위: `hirelog-web`, `hirelog-api`

## 최근 완료
- [x] 헤더 `홈` 메뉴 제거 (로고 클릭으로 홈 이동 유지)
- [x] 리뷰 카드 레이아웃 정돈 (간격/정렬/시각적 그룹화)
- [x] 리뷰 별점 표시 로직 보정 (1점=반별, 2점=한별)
- [x] 리뷰 별점 선택 UI 가독성 개선
- [x] 저장/해제 API 연동 안정화 (멱등 처리 보강)

## 프론트 우선 TODO
- [ ] `JobSummaryDetailPage` 텍스트 인코딩/한글 리소스 정리
- [ ] 리뷰 영역 컴포넌트 분리 (`RatingStarsDisplay`, `ScoreSelector`)
- [ ] 모바일 리뷰 카드 spacing 재점검

## 백엔드 우선 TODO
- [ ] 상태 전이 규칙을 서비스/도메인/문서에서 동일하게 고정
- [ ] `MemberJobSummary` 미존재 케이스에 대한 테스트 커버리지 확대
- [ ] 에러 코드/메시지 스펙 문서화

## 공통 TODO
- [ ] OpenAPI 예시 요청/응답 최신화
- [ ] QA 체크리스트 갱신 (저장/해제/준비기록/리뷰)
