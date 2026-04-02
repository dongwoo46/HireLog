# Python Folder Map (`src`)

이 문서는 `preprocess-pipeline/src`의 폴더 역할을 빠르게 파악하기 위한 맵입니다.

## 엔트리포인트
- `src/main_kafka.py`
  - 실제 운영 진입점.
  - 멀티프로세스(`ocr-process`, `text-url-process`)로 워커 구동.
- `src/main.py`
  - 단일 실행/로컬 테스트용 진입점 성격.

## 최상위 폴더 역할
- `src/inputs`
  - Kafka/Redis 메시지 파싱.
  - `JdPreprocessInput` 같은 내부 DTO 생성.
- `src/preprocess`
  - 전처리 핵심 도메인 로직.
  - URL/TEXT/OCR 파이프라인과 canonical 생성 흐름 포함.
- `src/url`
  - URL 수집/파싱 전담.
  - fetcher, parser, section strategy 분리 구조.
- `src/ocr`
  - OCR 실행 파이프라인.
  - 이미지 전처리/라인화/헤더 감지/품질 필터링 포함.
- `src/worker`
  - 런타임 워커(consumer loop) 구현.
  - source(TEXT/OCR/URL)별 worker 클래스.
- `src/infra`
  - 외부 인프라 어댑터.
  - Kafka/Redis client, config 로더.
- `src/outputs`
  - 전처리 결과 payload 포맷 생성.
- `src/domain`
  - 플랫폼 enum, 공통 도메인 타입.
- `src/normalize`
  - 텍스트 normalize 파이프라인.
- `src/common`
  - 공용 키워드/로더/상수류.
- `src/utils`
  - 로깅/백업 등 유틸리티.

## `preprocess` 상세
- `src/preprocess/worker/pipeline`
  - 파이프라인 오케스트레이션.
  - `url_pipeline.py`: URL 흐름 총괄.
  - `ocr_pipeline.py`: OCR source 전용 흐름.
  - `canonical_section_pipeline.py`: 섹션 -> canonical 변환.
  - `jobkorea_url_support.py`: JobKorea 전용 URL normalize/OCR fallback.
- `src/preprocess/adapter`
  - URL/OCR 섹션 데이터를 공통 Section 도메인으로 변환.
- `src/preprocess/metadata_preprocess`
  - 문서 메타(공고기간/스킬 등) 추출.
- `src/preprocess/post_validation`
  - 섹션 후검증/보정.
- `src/preprocess/semantic`, `src/preprocess/structural_preprocess`, `src/preprocess/core_preprocess`
  - 섹션/의미/문자열 전처리 세부 단계.

## `url` 상세
- `src/url/fetcher.py`
  - requests 기반 기본 fetch + JS 필요성 판단.
- `src/url/playwright_fetcher.py`
  - 공통 Playwright fetch.
- `src/url/fetchers/saramin_playwright_fetcher.py`
  - 사람인 전용 동적 fetch.
- `src/url/parsers`
  - 플랫폼별 HTML 파서.
  - `parser.py`(generic), `saramin_parser.py`, `jobkorea_parser.py`
- `src/url/section_strategies`
  - 플랫폼별 섹션 분류 전략.
  - `generic.py`, `wanted.py`, `remember.py`, `saramin.py`, `jobkorea.py`
- `src/url/preprocessor.py`
  - URL 텍스트 라인 클리닝 + 플랫폼 전략 라우팅.

## 현재 핵심 분기(요약)
- 플랫폼 판별: `JobPlatform.from_url(...)`
- 사람인: 전용 fetcher + 전용 parser + 전용 section 전략
- 잡코리아: 전용 parser + 전용 section 전략 + 전용 support(`jobkorea_url_support.py`)
  - `GI_Read -> GI_Read_Comt_Ifrm` URL 정규화
  - 이미지형 문서 OCR fallback 처리

## 파일 찾기 빠른 기준
- “어떤 URL을 어떻게 가져오나?” -> `src/url/fetcher.py`, `src/url/fetchers/*`
- “왜 섹션이 이렇게 나뉘나?” -> `src/url/section_strategies/*`
- “canonicalMap이 왜 비나?” -> `src/preprocess/worker/pipeline/url_pipeline.py`, `canonical_section_pipeline.py`
- “OCR가 왜/언제 도나?” -> `src/ocr/pipeline.py`, `src/preprocess/worker/pipeline/ocr_pipeline.py`, `jobkorea_url_support.py`

## 전처리 프로세스 순서(폴더/파일 기준)

### 1) 프로세스 시작
1. `src/main_kafka.py`
2. `src/infra/config/*`에서 Kafka/Worker 설정 로드
3. 워커 프로세스 생성
- `ocr-process`
- `text-url-process` (TEXT + URL)

### 2) 메시지 소비
1. `src/infra/kafka/kafka_consumer.py`로 토픽 메시지 poll
2. `src/worker/*_kafka_worker.py` 또는 `src/preprocess/worker/kafka/*`에서 source별 worker 실행

### 3) 입력 파싱
1. `src/inputs/parse_kafka_jd_preprocess.py` (또는 parse 모듈들)
2. `src/inputs/jd_preprocess_input.py` DTO 생성

### 4) source별 파이프라인 진입
1. URL source -> `src/preprocess/worker/pipeline/url_pipeline.py`
2. IMAGE source -> `src/preprocess/worker/pipeline/ocr_pipeline.py`
3. TEXT source -> `src/preprocess/worker/pipeline/text_preprocess_pipeline.py`

### 5) URL 파이프라인 내부 순서
1. 플랫폼 추론: `src/domain/job_platform.py`
2. URL fetch: `src/url/fetcher.py` (+ 필요시 `src/url/playwright_fetcher.py`)
3. 플랫폼 전용 fetch (예: 사람인): `src/url/fetchers/*`
4. HTML parse: `src/url/parsers/*`
5. URL 텍스트 전처리: `src/url/preprocessor.py`
6. 플랫폼 섹션 전략: `src/url/section_strategies/*`
7. Section 변환: `src/preprocess/adapter/url_section_adapter.py`
8. Canonical 생성: `src/preprocess/worker/pipeline/canonical_section_pipeline.py`
9. 메타 추출: `src/preprocess/metadata_preprocess/*`
10. 후검증: `src/preprocess/post_validation/*`

### 6) JobKorea 특수 흐름
1. URL 정규화 + OCR fallback 조정: `src/preprocess/worker/pipeline/jobkorea_url_support.py`
2. 필요 시 OCR 호출: `src/ocr/pipeline.py`
3. OCR 라인 헤더 섹션화: `src/ocr/structure/header_grouping.py`
4. OCR 섹션 도메인 변환: `src/preprocess/adapter/ocr_section_adapter.py`

### 7) OCR 파이프라인 내부 순서(IMAGE source)
1. OCR 실행: `src/ocr/pipeline.py`
2. OCR 구조화: `src/ocr/structure/header_grouping.py`
3. Section 변환: `src/preprocess/adapter/ocr_section_adapter.py`
4. Canonical/Metadata/Validation: `src/preprocess/worker/pipeline/ocr_pipeline.py`

### 8) 결과 출력
1. 결과 payload 조립: `src/outputs/*`
2. Kafka publish: `src/infra/kafka/kafka_producer.py`
3. 실패 시 fail topic publish: worker base 경로
