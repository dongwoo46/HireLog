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

### 전처리 로직 플랫폼별 분기
→ `preprocess-pipeline/src/url/platforms/` 에 파일 추가
→ `preprocessor.py`의 `_get_platform_module()` 분기 추가
→ 상세: `.claude/features/preprocess-pipeline/platform-based-preprocessing.md`

---

## 코딩 규칙

### Spring Boot / Kotlin
- enum 추가 시 Python `domain/` 에도 동일하게 추가
- Kafka 메시지 필드 추가 시 Python parse 로직도 함께 수정
- platform 없는 구 메시지는 `OTHER`로 fallback

### Python preprocess-pipeline
- 플랫폼 전용 로직은 `url/platforms/{platform}.py`에 격리
- `preprocessor.py`에 플랫폼 전용 코드 직접 작성 금지

### Frontend
- 새 플랫폼 추가 시 `types/jobSummary.ts`의 `JobPlatformType`, `JOB_PLATFORM_LABELS` 모두 업데이트
