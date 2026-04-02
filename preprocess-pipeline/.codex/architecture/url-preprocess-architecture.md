# URL Preprocess Architecture (Current)

## 핵심 흐름
1. `UrlPipeline`이 URL 기반으로 플랫폼을 추론한다 (`JobPlatform.from_url`).
2. URL fetch 후 parser 선택:
- SARAMIN: `SaraminPlaywrightFetcher` + `SaraminUrlParser`
- 그 외: 기본 fetcher/parser
3. 플랫폼별 section strategy 실행.
4. canonical 변환 및 후처리.

## 폴더 역할
- `src/url/fetchers/`: 동적/플랫폼 특화 fetch 로직
- `src/url/parsers/`: HTML -> body/title 파싱
- `src/url/section_strategies/`: 라인 기반 섹션 분류
- `src/preprocess/worker/pipeline/url_pipeline.py`: URL 파이프라인 오케스트레이션

## 사람인 분기 포인트
- fetch 단계: 사람인 전용 fetcher 사용
- parse 단계: 사람인 전용 parser 사용
- canonical 단계: 사람인 intake 키 보정(`responsibilities`, `requirements`, `preferred`)
