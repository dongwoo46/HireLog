# 플랫폼 기반 URL 전처리 분리

**작업일**: 2026-03-30
**이슈**: 리멤버 파싱 개선 로직이 원티드 섹션 분리를 깨뜨림

---

## 문제 원인

원티드 채용공고 전처리 결과 `canonicalMap`에 `intro`, `experience`만 나오고
`주요업무`, `자격요건`, `우대사항` 등 섹션 분리가 안 되는 문제.

리멤버 대응으로 추가했던 `_remove_menu_fragments` 로직이 원인:
- "5개 이상 연속 짧은 라인 + 전부 header keyword → 탭 nav로 판단 → 전부 제거"
- 원티드는 섹션 헤더가 본문 중간에 연속으로 등장 → 이 로직에 걸려 전부 제거됨

---

## 해결 방향

플랫폼마다 HTML 구조가 달라 단일 휴리스틱 불가 → **플랫폼별 전처리 전략 분리**

---

## 아키텍처

```
url/preprocessor.py          ← 진입점 (platform 기반 dispatch)
url/platforms/
  __init__.py
  generic.py                 ← 기본 전략
  wanted.py                  ← 원티드 전용
  remember.py                ← 리멤버 전용
```

### 각 플랫폼 모듈이 제공하는 인터페이스

```python
def remove_menu_fragments(lines: List[str]) -> List[str]: ...
def allow_header_keyword_dedup() -> bool: ...
def get_ui_noise_patterns() -> List[str]: ...
```

---

## 플랫폼별 전략 차이

### wanted.py
- `remove_menu_fragments`: 연속 5개+ 버퍼에서 header keyword 라인만 보존 (제거 안 함)
- `allow_header_keyword_dedup`: False
- 고유 노이즈: `채용중인 포지션`, `연봉상위`, `팔로우하고 채용알림` 등

### remember.py
- `remove_menu_fragments`: 5개+ 전부 header → **전부 제거** (탭 nav 판단)
- `allow_header_keyword_dedup`: True (탭 nav + 본문 헤더 중복 등장)
- 고유 노이즈: `합격 보상금`, `이 포지션에 합격해 입사하시면`, `현직자에게` 등

### generic.py
- `remove_menu_fragments`: 5개+ 버퍼에서 header keyword 라인만 보존
- `allow_header_keyword_dedup`: False
- 고유 노이즈: 없음

---

## 수정 파일 상세

### preprocess-pipeline

| 파일 | 변경 |
|---|---|
| `src/domain/job_platform.py` | 신규 — JobPlatform enum (13개 플랫폼) |
| `src/inputs/jd_preprocess_input.py` | `platform: JobPlatform = JobPlatform.OTHER` 추가 |
| `src/inputs/parse_jd_preprocess_message.py` | `payload.platform` 파싱 추가 |
| `src/url/platforms/__init__.py` | 신규 (패키지) |
| `src/url/platforms/generic.py` | 신규 |
| `src/url/platforms/wanted.py` | 신규 |
| `src/url/platforms/remember.py` | 신규 |
| `src/url/preprocessor.py` | 전면 재작성 — platform dispatch 구조 |
| `src/preprocess/worker/pipeline/url_pipeline.py` | `preprocess_url_text(body_text, platform=input.platform)` 전달 |

### Spring Boot (hirelog-api)

| 파일 | 변경 |
|---|---|
| `job/domain/type/JobPlatformType.kt` | 신규 — 13개 플랫폼 enum |
| `dto/request/JobSummaryTextReq.kt` | `platform: JobPlatformType` 추가 |
| `dto/request/JobSummaryUrlReq.kt` | `platform: JobPlatformType` 추가 |
| `dto/request/JobSummaryOcrReq.kt` | `platform: JobPlatformType` 추가 |
| `messaging/JdPreprocessRequestMessage.kt` | `platform: JobPlatformType` 추가 |
| `intake/JdIntakeService.kt` | requestText/Ocr/Url에 platform 파라미터 추가 |
| `presentation/controller/JobSummaryController.kt` | 3개 엔드포인트 platform 전달 |

### Frontend (hirelog-web)

| 파일 | 변경 |
|---|---|
| `src/types/jobSummary.ts` | `JobPlatformType` union type, `JOB_PLATFORM_LABELS` 추가 |
| `src/pages/JobSummaryRequestPage.tsx` | platform select 드롭다운 UI 추가 |
| `src/services/jdSummaryService.ts` | requestOcr FormData에 platform 추가 |

---

## 신규 플랫폼 추가 방법

1. `src/url/platforms/{name}.py` 생성 (generic.py 복사 후 전략 수정)
2. `src/url/preprocessor.py`의 `_get_platform_module()` 분기 추가
3. `src/domain/job_platform.py` enum 값 추가
4. Spring `JobPlatformType.kt` enum 값 추가
5. Frontend `JobPlatformType`, `JOB_PLATFORM_LABELS` 추가
