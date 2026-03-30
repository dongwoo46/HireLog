# 키워드 파일 구조

## 위치
`src/common/section/`

## 파일 목록

| 파일 | 반환 타입 | 용도 |
|---|---|---|
| `header_keywords.yml` | `set[str]` | 헤더 판별 (is_text_header_candidate, _get_matched_header_keyword) |
| `section_keywords.yml` | `dict[str, list[str]]` | semantic zone 매핑 (detect_semantic_zone) |
| `jd_meta_keywords.yml` | `set[str]` | 전형절차/메타 정보 판별 |

모두 `@lru_cache(maxsize=1)` 로 캐싱됨.
키워드 파일 수정 후 반영하려면 `clear_keyword_cache()` 호출 필요.

---

## header_keywords.yml

헤더로 인식할 키워드 목록 (flat list).
비교 시 공백 제거 + 소문자 변환.

### 포함된 스타일
- 한국어 단어형: `주요업무`, `자격요건`, `우대사항`, `채용절차` 등
- 한국어 공백 있음: `주요 업무`, `자격 요건`, `우대 사항` 등
- 영어: `responsibilities`, `requirements`, `preferred`, `qualifications` 등
- 토스/배민 스타일 문장형: `함께할 업무`, `이런 분을 찾고 있어요`, `이런 경험이 있으면 좋아요` 등

### 최근 추가
- `기타안내`, `기타 안내` (Remember 플랫폼 고유 레이블)

---

## section_keywords.yml

canonical section name → 키워드 리스트 매핑.
detect_semantic_zone에서 사용.

```yaml
responsibilities:
  - 담당업무 / 주요업무 / 역할 / role / Key Responsibilities 등
requiredQualifications:
  - 자격요건 / 필수요건 / requirements / Basic Qualifications 등
preferredQualifications:
  - 우대사항 / 우대조건 / preferred / Preferred Qualifications 등
recruitmentProcess:
  - 채용절차 / 전형절차 + 30여 개 변형
```

### _SECTION_KEYWORD_MAP (semantic_zone.py 하드코딩)
```python
"summary"                → "company"
"responsibilities"       → "responsibilities"
"requiredQualifications" → "requirements"
"preferredQualifications"→ "preferred"
"skills"                 → "skills"
"experience"             → "experience"
"recruitmentProcess"     → "process"
"applicationGuide"       → "application_guide"
"employmentType"         → "employment_type"
"location"               → "location"
"benefits"               → "benefits"
"etc"                    → "others"
```

---

## 키워드 매칭 동작 특성

### URL (`_get_matched_header_keyword`)
- 입력: 원본 라인 문자열
- 정규화: `stripped.lower().replace(" ", "")`
- `[...]` → 대괄호 제거 후 비교
- 부분 일치 허용
- `len(kw) >= 6`이면 문장형 검사 스킵
- **픽스**: `[bracket]` + `len(kw) < 6` + 부분일치 → skip

### TEXT/OCR (`is_text_header_candidate`)
- 완전 일치: `lowered in header_keywords`
- 부분 일치: `keyword in lowered` or `kw_no_space in lowered_no_space`
- **픽스**: `[bracket]` + `len(kw) < 6` + 부분일치 → skip

### semantic zone (`_matches_keywords`)
- 정규화: `lower().replace(" ", "").strip()`
- 양방향 부분 일치: `kw in header` or `header in kw`
- coverage 체크 없음 (validate의 `_matches_any_keyword`와 다름)

### post_validation (`_matches_any_keyword`)
- **coverage ≥ 40%** 조건 추가 (오탐 방지 목적)
- `coverage = len(kw_norm) / len(normalized)`
