# Canonical Section Pipeline (공통 후반부)

URL / TEXT / OCR 세 파이프라인이 모두 이 단계를 공통으로 사용한다.

## 흐름

```
list[Section]
 └─ CanonicalSectionPipeline.process()
     ├─ apply_semantic_lite()         ← semantic_zone 태깅
     ├─ filter_irrelevant_sections()  ← DROP 대상 섹션 제거
     └─ _build_canonical_map()        → dict[zone, list[str]]
```

**파일**: `preprocess/worker/pipeline/canonical_section_pipeline.py`

---

## apply_semantic_lite

**파일**: `preprocess/semantic/semantic_preprocessor.py`

- `semantic_zone == "others"` 인 섹션만 처리
- `detect_semantic_zone(sec.header)` 호출

## detect_semantic_zone

**파일**: `preprocess/semantic/semantic_zone.py`

헤더 문자열만으로 zone 판별. 내용(lines, lists) 절대 참조 안 함.

### 판별 우선순위 (순서 중요)
```
1. responsibilities  ← 주요업무, 담당업무, Key Responsibilities, role 등
2. preferred         ← 우대사항, Preferred Qualifications 등 (requirements보다 먼저!)
3. requirements      ← 자격요건, 필수요건, Basic Qualifications 등
4. experience        ← 경력, 경험
5. company           ← 회사소개, 조직소개
6. benefits          ← 복지, 혜택
7. application_questions
8. application_guide
9. process           ← 채용절차, 전형절차 (30여 개)
10. skills           ← 기술스택
11. employment_type  ← 고용형태
12. location         ← 근무지
→ "others"
```

### _matches_keywords 정규화
```python
text.lower().replace(" ", "").strip()
```
부분 일치 허용 (`kw in header` or `header in kw`)

---

## filter_irrelevant_sections

**파일**: `preprocess/semantic/section_filter.py`

```python
DROP_SECTION_HEADERS = {
    "유의사항", "마감일", "참고사항", "안내사항", "기타사항",
    "notice", "disclaimer"
}
```
완전 일치인 경우만 제거 (복합 헤더는 제거 안 함)

---

## _build_canonical_map

```python
for sec in sections:
    zone = sec.semantic_zone
    for line in sec.lines:
        result[zone].append(line)
    for lst in sec.lists:
        for item in lst:
            result[zone].append(item)
```

같은 zone의 섹션이 여러 개면 모두 합산됨.

---

## validate_raw_sections / validate_section_objects

**파일**: `post_validation/section_post_validator.py`

### Rule 1: intro 흡수
- 첫 keyword-match 섹션 이전의 미매칭 헤더 → `__intro__` 강제 편입
- `_matches_any_keyword()`: coverage ≥ 40% 조건 (오탐 방지)
  ```python
  coverage = len(kw_norm) / len(normalized)  # ≥ 0.4 이어야 매칭
  ```
  e.g. `[MissionoftheRole]` → "role" coverage = 4/16 = 25% < 40% → **intro 흡수됨**
  e.g. `[KeyResponsibilities]` → "responsibilities" coverage = 16/19 = 84% → **키워드 매칭**

### Rule 2: 빈 header 병합
- content 0줄 header → 다음 섹션들을 content로 흡수 (체인 처리 가능)
- 핵심: Remember JD처럼 `주요업무(0줄) → [MissionoftheRole] → [KeyResponsibilities]` 체인 처리

### Rule 3: 푸터 노이즈 제거
- 마지막 섹션 끝 연속 짧은 라인 (≤15자, 2개 이상) 제거

---

## canonical_map 최종 구조 (Spring 기대값)

| key | zone 값 | Spring 검증 |
|---|---|---|
| `responsibilities` | 주요업무 관련 | `isNullOrEmpty()` 차단 |
| `requirements` | 자격요건 관련 | `isNullOrEmpty()` 차단 |
| `preferred` | 우대사항 관련 | `isNullOrEmpty()` 차단 ← 자주 누락 |
| `process` | 채용절차 | 검증 없음 |
| `company` | 회사소개 | 검증 없음 |

Spring isValidJd() 추가 조건:
- `canonicalText` (전체 합산) 길이 < 300 → 차단
