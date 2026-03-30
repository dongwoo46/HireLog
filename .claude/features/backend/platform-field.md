# JobPlatformType — 채용 플랫폼 필드 추가

**작업일**: 2026-03-30
**연관**: preprocess-pipeline platform-based-preprocessing

---

## 목적

플랫폼마다 HTML 구조가 달라 전처리 로직 분기 필요.
클라이언트(프론트)가 플랫폼을 명시적으로 전달 → 파이프라인 전체에 흐름.

---

## 지원 플랫폼 (JobPlatformType.kt)

```kotlin
enum class JobPlatformType {
    WANTED, REMEMBER, SARAMIN, JOBKOREA,
    ROCKETPUNCH, PROGRAMMERS, JUMPIT, RALLIT,
    CATCH, INCRUIT, GREPP, LINKEDIN, OTHER
}
```

---

## 데이터 흐름

```
Frontend (platform select)
  → POST /api/job-summary/{text|url|ocr} (platform 필드 포함)
  → JobSummaryController
  → JdIntakeService.request{Text|Ocr|Url}(platform=...)
  → JdPreprocessRequestMessage (platform 필드)
  → Outbox → Debezium → Kafka
  → Python preprocess-pipeline (platform 기반 전처리 분기)
```

---

## 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `job/domain/type/JobPlatformType.kt` | 신규 생성 |
| `dto/request/JobSummaryTextReq.kt` | `platform: JobPlatformType` 추가 (`@NotNull`) |
| `dto/request/JobSummaryUrlReq.kt` | `platform: JobPlatformType` 추가 (`@NotNull`) |
| `dto/request/JobSummaryOcrReq.kt` | `platform: JobPlatformType` 추가 (default `OTHER`) |
| `messaging/JdPreprocessRequestMessage.kt` | `platform: JobPlatformType` 필드 추가 |
| `intake/JdIntakeService.kt` | requestText/requestOcr/requestUrl 파라미터에 platform 추가 |
| `presentation/controller/JobSummaryController.kt` | 3개 엔드포인트 모두 platform 전달 |

---

## 주의사항

- OCR (`JobSummaryOcrReq`)은 multipart/form-data라 `platform`을 form field로 전송
- Kafka 메시지 (`JdPreprocessRequestMessage`)에 `platform` 직렬화됨 → Python이 `payload.platform`으로 수신
- 기존 메시지에 platform 없는 경우 Python에서 `OTHER`로 fallback
