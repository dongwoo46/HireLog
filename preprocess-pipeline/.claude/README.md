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

## 핵심 제약 사항

1. **`preferred` 섹션 누락이 Spring에서 파이프라인 차단** → 가장 자주 발생하는 이슈
2. **URL 파이프라인**: `_get_matched_header_keyword` 에서 short keyword + bracket 오탐 주의
3. **TEXT/OCR 파이프라인**: `is_text_header_candidate` Rule 6 (bullet follows) → 한국어 소제목이 헤더로 잘못 판정될 수 있음
4. **validate Rule 1**: coverage < 40% 섹션은 intro로 흡수됨 → 의도치 않은 섹션 손실 주의
5. **캐시**: 키워드 YML 수정 후 `clear_keyword_cache()` 또는 프로세스 재시작 필요
