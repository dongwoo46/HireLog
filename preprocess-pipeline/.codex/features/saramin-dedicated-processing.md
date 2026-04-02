# Feature: Saramin Dedicated URL Processing

## 배경
사람인 `relay/view` URL은 shell/iframe 구조라 일반 static 파싱에서 본문 누락이 자주 발생.

## 구현 사항
- 전용 fetcher 추가: `src/url/fetchers/saramin_playwright_fetcher.py`
- 전용 parser 추가: `src/url/parsers/saramin_parser.py`
- 플랫폼 자동 추론 기반 분기 적용 (`platform` 입력 의존 제거)

## Fetch 전략 (사람인)
1. URL 정규화: `view -> view-detail` 변환 (`rec_idx`, `rec_seq` 유지)
2. static chain 우선:
- view/view-detail HTML 요청
- iframe src 추출
- iframe HTML 직접 요청
3. 필요 시 Playwright fallback:
- `commit` 우선 navigation
- frame/main DOM에서 본문 출현 대기

## Parser 전략 (사람인)
- 구조 보존 추출: heading/table/list 기반 라인 생성
- 테이블 flatten (`header: value`)
- 노이즈 필터링(사람인 안내/도움말 등)

## Canonical 보정
- Saramin 전용 canonical normalize 적용
- intake 최소 키 보전:
- `responsibilities`
- `requirements`
- `preferred`
