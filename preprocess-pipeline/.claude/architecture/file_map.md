# 파일 맵 (역할별)

## Worker 진입점

```
src/
├─ main.py / main_kafka.py           # 프로세스 시작
├─ worker/
│   ├─ url_stream_worker.py          # URL Redis stream 소비
│   ├─ text_stream_worker.py         # TEXT Redis stream 소비
│   └─ ocr_stream_worker.py          # OCR Redis stream 소비
└─ preprocess/worker/
    ├─ redis/
    │   ├─ jd_preprocess_url_worker.py    # URL 파이프라인 실행 + 결과 발행
    │   ├─ jd_preprocess_text_worker.py
    │   └─ jd_preprocess_ocr_worker.py
    ├─ kafka/
    │   └─ kafka_jd_preprocess_url_worker.py  # Kafka 버전
    └─ pipeline/
        ├─ url_pipeline.py                # URL 단계 조합
        ├─ text_preprocess_pipeline.py    # TEXT 단계 조합
        ├─ ocr_pipeline.py                # OCR 단계 조합
        └─ canonical_section_pipeline.py  # 공통 후반 파이프라인
```

## URL 파이프라인 전용

```
src/url/
├─ fetcher.py              # requests 정적 fetch
├─ playwright_fetcher.py   # JS 렌더링 + 더보기 클릭
├─ parser.py               # HTML → 텍스트 (score-based best-candidate)
├─ preprocessor.py         # UI 노이즈 제거
└─ section_extractor.py    # 헤더 기반 섹션 분리
```

## TEXT/OCR 공통 전처리

```
src/preprocess/
├─ core_preprocess/
│   ├─ core_preprocessor.py       # 5단계 조합
│   ├─ input_normalizer.py
│   ├─ noise_filter.py             # UI 노이즈 제거
│   ├─ line_segmenter.py           # 개행 분리
│   ├─ bullet_normalizer.py        # bullet 통일
│   └─ text_damage_guard.py
├─ structural_preprocess/
│   ├─ structural_preprocessor.py  # build_sections + group_lists
│   ├─ section_builder.py          # Section dataclass, build_sections()
│   ├─ header_detector.py          # is_text_header_candidate() ← TEXT/OCR 헤더 판별
│   └─ list_grouper.py             # bullet 리스트 그룹핑
├─ semantic/
│   ├─ semantic_preprocessor.py    # apply_semantic_lite()
│   ├─ semantic_zone.py            # detect_semantic_zone() ← zone 매핑
│   └─ section_filter.py           # filter_irrelevant_sections()
├─ metadata_preprocess/
│   └─ metadata_preprocessor.py    # 날짜, 스킬, 문서 메타 추출
├─ post_validation/
│   └─ section_post_validator.py   # Rule 1/2/3 후보정
└─ adapter/
    ├─ url_section_adapter.py      # dict → Section 변환 (URL용)
    └─ ocr_section_adapter.py      # OCR 구조 → Section 변환
```

## 공통 키워드/설정

```
src/common/
├─ section/
│   ├─ loader.py                   # YAML 로드 + lru_cache
│   ├─ header_keywords.yml         # 헤더 판별 키워드 ← 수정 빈번
│   ├─ section_keywords.yml        # zone 매핑 키워드
│   └─ jd_meta_keywords.yml        # 메타 키워드
├─ noise/                          # UI 노이즈 패턴
├─ token/
└─ vocab/
```

## 인프라

```
src/infra/
├─ redis/
│   ├─ redis_client.py
│   ├─ stream_consumer.py
│   ├─ stream_publisher.py
│   └─ stream_keys.py              # Redis stream key 상수
├─ kafka/
│   ├─ kafka_consumer.py
│   └─ kafka_producer.py
└─ config/
    ├─ redis_config.py
    └─ kafka_config.py
```

## OCR 전용

```
src/ocr/
├─ engine.py
├─ pipeline.py
├─ preprocess.py
├─ header_detector.py              # OCR 전용 헤더 감지
├─ structure/
│   ├─ header_grouping.py
│   └─ is_header.py
└─ ...
```
