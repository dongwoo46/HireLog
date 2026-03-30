# HireLog 프로젝트 개발 규칙

## 프로젝트 구조

```
HireLog/
├── hirelog-api/          ← Spring Boot / Kotlin 백엔드
├── hirelog-web/          ← React / TypeScript 프론트엔드
└── preprocess-pipeline/  ← Python JD 전처리 파이프라인
```

---

## 전체 데이터 흐름

```
hirelog-web (React)
  → REST API (hirelog-api, Spring Boot)
  → Outbox → Debezium → Kafka
  → preprocess-pipeline (Python, 3개 worker: URL/TEXT/OCR)
  → Kafka response
  → hirelog-api consumer
  → LLM 요약
  → OpenSearch 인덱싱
```

---

## 신규 기능 추가 시 수정 위치

### 요청 필드 추가
1. `hirelog-api`: 요청 DTO → JdPreprocessRequestMessage → JdIntakeService → Controller
2. `hirelog-web`: `types/jobSummary.ts` → 해당 서비스 → 페이지 UI
3. `preprocess-pipeline`: `inputs/jd_preprocess_input.py` → `parse_jd_preprocess_message.py`

### 전처리 및 섹션 분리 플랫폼별 분기 (URL only)
→ `preprocess-pipeline/src/url/platforms/` 에 파일 추가
→ `preprocessor.py`의 `get_platform_module()` 분기 추가
→ 각 플랫폼 모듈에 `get_ui_noise_patterns`, `remove_menu_fragments`, `extract_sections` 구현
→ TEXT/OCR은 platform 무관 (platform 필드 사용 안 함)
→ 상세: `.claude/features/preprocess-pipeline/platform-based-preprocessing.md`

---

## 코딩 규칙

### Spring Boot / Kotlin
- enum 추가 시 Python `domain/` 에도 동일하게 추가
- Kafka 메시지 필드 추가 시 Python parse 로직도 함께 수정
- `platform` 필드는 URL 요청에만 존재, `JdPreprocessRequestMessage.platform`은 nullable
- platform 없는 Kafka 메시지(TEXT/OCR)는 Python에서 `OTHER`로 폴백

### Python preprocess-pipeline
- 플랫폼 전용 로직은 `url/platforms/{platform}.py`에 격리
- `preprocessor.py`에 플랫폼 전용 코드 직접 작성 금지
- TEXT/OCR 파이프라인은 platform 필드를 사용하지 않음

### Frontend
- 새 플랫폼 추가 시 `types/jobSummary.ts`의 `JobPlatformType`, `JOB_PLATFORM_LABELS` 모두 업데이트
- platform UI는 URL 요청 폼에만 존재 (TEXT/OCR 폼에는 없음)
