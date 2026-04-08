# .claude — preprocess-pipeline 개발 참고 문서

코드 전체를 다시 읽지 않고 수정하기 위한 참고 자료 모음.

## 폴더 구조

```
.claude/
├─ README.md                    ← 이 파일 (목차)
├─ pipeline/
│   ├─ url_pipeline.md          ← URL 파이프라인 전체 흐름 + 특성
│   ├─ text_ocr_pipeline.md     ← TEXT/OCR 파이프라인 흐름 + header_detector 특성
│   └─ canonical_pipeline.md    ← 공통 후반부 (semantic zone, validate, canonical_map)
├─ keywords/
│   └─ keywords_structure.md    ← 키워드 파일 구조 + 매칭 동작 차이
├─ sites/
│   └─ remember.md              ← 리멤버 사이트 특성 + 알려진 이슈 + 픽스 내역
└─ architecture/
    ├─ data_flow.md             ← 전체 데이터 흐름, DTO, zone 목록, Spring 검증 조건
    └─ file_map.md              ← 파일별 역할 맵
```

## 자주 수정하는 파일

| 수정 목적 | 파일 |
|---|---|
| 새 사이트 헤더 인식 추가 | `src/common/section/header_keywords.yml` |
| zone 매핑 변경 | `src/common/section/section_keywords.yml` |
| URL 헤더 판별 로직 | `src/url/section_extractor.py` `_get_matched_header_keyword()` |
| TEXT/OCR 헤더 판별 로직 | `src/preprocess/structural_preprocess/header_detector.py` |
| 새 사이트 파서 특성 | `src/url/parser.py` |
| UI 노이즈 패턴 추가 | `src/url/preprocessor.py` `UI_NOISE_EXACT` / `UI_NOISE_PATTERNS` |
| 섹션 필터 DROP 대상 추가 | `src/preprocess/semantic/section_filter.py` `DROP_SECTION_HEADERS` |
| 후보정 규칙 변경 | `src/preprocess/post_validation/section_post_validator.py` |

## 알려진 버그 및 해결 이력

### [2026-04-07] 멀티프로세스 환경에서 로그가 출력되지 않는 문제

**증상**
- `python src/main_kafka.py` 실행 시 OCR 프로세스(pid A)는 "Consumer created"까지만 찍히고 이후 로그 없음
- text_url 프로세스(pid B)는 JSON 로그가 단 한 줄도 찍히지 않음
- 전처리 결과(canonical_map)는 실제로 생성되고 Kafka publish도 됐지만 로그가 없어서 확인 불가

**원인 1: ppocr이 `logging.disable()` 호출**
- PaddleOCR은 첫 `.ocr()` 호출(모델 weight lazy load) 시 내부에서 `logging.disable()`을 호출함
- `_init_process_logging()`으로 JSON handler를 세팅해도 OCR 첫 추론 시점에 통째로 무력화됨
- **해결**: `main_kafka.py` `_run_ocr_process()`에서
  1. import 전 1차 `setup_logging()` 호출 (startup 로그용)
  2. import 후 2차 `setup_logging()` + `logging.disable(logging.NOTSET)` 호출 (ppocr import 개입 override)
  3. 워밍업 더미 추론(`np.zeros(64,64,3)`)으로 모델 로드를 worker.run() 전에 완료
  4. 워밍업 후 3차 `setup_logging()` + `logging.disable(logging.NOTSET)` 재적용

**원인 2: `jobkorea_url_support.py` 모듈 레벨 OCR import**
- `from ocr.pipeline import process_ocr_input`이 모듈 최상단에 있어서
  text_url 프로세스가 UrlKafkaWorker를 로드할 때 PaddleOCR 전체가 딸려옴
- ppocr import 시 root logger를 건드려 text_url 프로세스의 모든 INFO 로그가 차단됨
- **해결**: `ocr_only_result()`, `ocr_fallback_merge()` 메서드 내부로 import를 lazy하게 이동
  OCR 호출 후 즉시 `setup_logging()` + `logging.disable(logging.NOTSET)` 재적용

**원인 3: extra={"process": ...} LogRecord 예약 필드 충돌**
- Python `LogRecord`는 `process`를 PID용 예약 필드로 사용
- `extra={"process": "ocr"}` 전달 시 `KeyError: "Attempt to overwrite 'process' in LogRecord"` 발생
- **해결**: `"process"` → `"proc"`으로 전체 치환 (`main_kafka.py`)

**원인 4: `logger.info("Preprocess completed", extra={"preprocess_result": result.to_dict()})`**
- canonical_map 전체를 extra에 포함하면 로그 1줄이 수만 바이트가 됨
- Promtail 기본 최대 크기 초과 시 해당 라인 드랍
- **해결**: `section_count`, `has_skills`, `has_period` 등 메타만 로그에 남김
  canonical_map 전체는 별도 DEBUG 레벨로 분리

---

## 핵심 제약 사항

1. **`preferred` 섹션 누락이 Spring에서 파이프라인 차단** → 가장 자주 발생하는 이슈
2. **URL 파이프라인**: `_get_matched_header_keyword` 에서 short keyword + bracket 오탐 주의
3. **TEXT/OCR 파이프라인**: `is_text_header_candidate` Rule 6 (bullet follows) → 한국어 소제목이 헤더로 잘못 판정될 수 있음
4. **validate Rule 1**: coverage < 40% 섹션은 intro로 흡수됨 → 의도치 않은 섹션 손실 주의
5. **캐시**: 키워드 YML 수정 후 `clear_keyword_cache()` 또는 프로세스 재시작 필요
