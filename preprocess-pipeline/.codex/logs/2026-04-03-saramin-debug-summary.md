# 2026-04-03 Saramin Debug Log Summary

## 주요 이슈
- `Page.goto ... Timeout` / `ERR_CONNECTION_RESET`
- `chrome-error://chromewebdata/`로 navigation 중단
- shell 텍스트만 파싱되어 JD 본문 누락

## 원인 정리
- 사람인 페이지가 outer shell + iframe(view-detail) 구조
- 실행 환경에서 Playwright 네트워크 레벨 차단/리셋 발생 가능
- `domcontentloaded` 대기 신뢰도가 낮아 장시간 타임아웃 발생

## 대응
1. 사람인 전용 fetcher/parse/section 분리
2. Playwright 대기 전략 보강(commit 우선, 본문 출현 대기)
3. static chain 우선 경로 추가(iframe src 직접 요청)
4. canonical 필수 키 보정

## 확인 포인트(재테스트)
- `Saramin iframe src extracted`
- `Saramin static chain fetch succeeded`
- `parsed_length` 증가 및 `responsibilities/requirements/preferred` 채움 여부
