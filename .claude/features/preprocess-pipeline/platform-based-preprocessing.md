# 플랫폼 기반 URL 전처리 및 섹션 분리

**작업일**: 2026-03-30

---

## 배경

리멤버 파싱 개선 로직이 원티드 섹션 분리를 깨뜨림.
단일 휴리스틱으로는 플랫폼별 HTML 구조 차이를 처리 불가 → 플랫폼별 전략 분리.

---

## 아키텍처

```
url/preprocessor.py          ← get_platform_module(platform) dispatch
url/platforms/
  generic.py                 ← 기본 전략
  wanted.py                  ← 원티드 전용
  remember.py                ← 리멤버 전용
```

각 플랫폼 모듈 인터페이스:
```python
def get_ui_noise_patterns() -> List[str]: ...
def allow_header_keyword_dedup() -> bool: ...
def remove_menu_fragments(lines: List[str]) -> List[str]: ...
def extract_sections(lines: List[str]) -> dict: ...   ← 섹션 분리 플랫폼별 분기
```

`url_pipeline.py` 흐름:
```python
platform_mod = get_platform_module(input.platform)
cleaned_lines = preprocess_url_text(body_text, platform=input.platform)
raw_sections = platform_mod.extract_sections(cleaned_lines)
```

---

## platform 수신 범위

| source | platform 사용 | 비고 |
|---|---|---|
| URL | ✅ | 전처리 + 섹션 분리 모두 platform 분기 |
| TEXT | ❌ | platform 무관, 범용 전처리 |
| OCR | ❌ | platform 무관, OCR 전용 전처리 |

Spring → Kafka 메시지(`JdPreprocessRequestMessage`):
- URL: `platform` 필드 포함
- TEXT/OCR: `platform` 필드 미포함 (nullable)
- Python 파서: `message.get("platform", "OTHER")` → 없으면 OTHER 폴백

---

## 플랫폼별 전략

### wanted.py
- `remove_menu_fragments`: 연속 5개+ 버퍼에서 header keyword 라인만 보존
- `allow_header_keyword_dedup`: False
- `extract_sections`: 현재 generic과 동일 (추후 튜닝 예정)
- 고유 노이즈: `채용중인 포지션`, `연봉상위`, `팔로우하고 채용알림` 등

### remember.py
- `remove_menu_fragments`: 5개+ 전부 header → 탭 nav 판단 → 전부 제거
- `allow_header_keyword_dedup`: True (탭 nav + 본문 헤더 중복 등장)
- `extract_sections`: 현재 generic과 동일 (추후 튜닝 예정)
- 고유 노이즈: `합격 보상금`, `이 포지션에 합격해 입사하시면`, `현직자에게` 등

### generic.py
- `remove_menu_fragments`: 연속 5개+ 버퍼에서 header keyword 라인만 보존
- `allow_header_keyword_dedup`: False
- `extract_sections`: `extract_url_sections` 그대로

---

## 신규 플랫폼 추가 방법

1. `src/url/platforms/{name}.py` 생성 (generic.py 복사 후 전략 수정)
2. `src/url/preprocessor.py`의 `get_platform_module()` 분기 추가
3. `src/domain/job_platform.py` enum 값 추가
4. Spring `JobPlatformType.kt` enum 값 추가
5. Frontend `JobPlatformType`, `JOB_PLATFORM_LABELS` 추가 (URL 요청 UI에만)

---

## 수정 파일 목록

### preprocess-pipeline

| 파일 | 변경 |
|---|---|
| `src/inputs/kafka_jd_preprocess_input.py` | `platform: JobPlatform = JobPlatform.OTHER` 추가 |
| `src/inputs/parse_kafka_jd_preprocess.py` | `platform` 파싱 추가 (없으면 OTHER) |
| `src/url/preprocessor.py` | `get_platform_module()` 공개화, `preprocess_lines_by_platform()` 추가 |
| `src/url/platforms/generic.py` | `extract_sections()` 추가 |
| `src/url/platforms/wanted.py` | `extract_sections()` 추가 |
| `src/url/platforms/remember.py` | `extract_sections()` 추가 |
| `src/preprocess/worker/pipeline/url_pipeline.py` | `platform_mod.extract_sections()` 호출로 교체 |

### Spring Boot (hirelog-api)

| 파일 | 변경 |
|---|---|
| `messaging/JdPreprocessRequestMessage.kt` | `platform: JobPlatformType? = null` (nullable) |
| `dto/request/JobSummaryUrlReq.kt` | `platform` 유지 |
| `dto/request/JobSummaryTextReq.kt` | `platform` 제거 |
| `dto/request/JobSummaryOcrReq.kt` | `platform` 제거 |
| `intake/JdIntakeService.kt` | requestText/Ocr에서 platform 파라미터 제거 |
| `presentation/controller/JobSummaryController.kt` | TEXT/OCR 엔드포인트 platform 제거 |
