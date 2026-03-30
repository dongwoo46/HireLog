# TEXT / OCR 파이프라인

## TEXT 흐름

```
raw_text
 └─ CorePreprocessor.process()
     ├─ normalize_input_text()
     ├─ remove_ui_noise()          ← common/noise/ 키워드 기반
     ├─ segment_lines()            ← 개행 분리, 빈 줄 제거
     ├─ normalize_bullets()        ← bullet 표기 통일
     └─ guard_text_damage()        ← 깨진 텍스트 방어
         └─ StructuralPreprocessor.process()
             ├─ build_sections()             ← 헤더 기반 Section 목록
             │   └─ is_text_header_candidate()
             └─ group_lists()               ← bullet 그룹핑
                 └─ validate_section_objects()
                     └─ CanonicalSectionPipeline.process()
```

## OCR 흐름

```
이미지
 └─ OCR Engine (PaddleOCR)
     └─ OCR 후처리 (ocr/ 패키지)
         └─ StructuralPreprocessor 이후 TEXT와 동일
```

## 주요 파일

| 파일 | 역할 |
|---|---|
| `preprocess/core_preprocess/core_preprocessor.py` | Core 5단계 조합 |
| `preprocess/structural_preprocess/section_builder.py` | `build_sections()`, Section dataclass |
| `preprocess/structural_preprocess/header_detector.py` | `is_text_header_candidate()` |
| `preprocess/structural_preprocess/list_grouper.py` | bullet 리스트 그룹핑 |

## is_text_header_candidate 판별 순서

```
1. bullet(•) 시작 → False
2. header_keywords 키워드 매칭 (부분 일치)
   ├─ [픽스] bracket + len(kw) < 6 + 부분일치 → skip
   └─ 매칭 → True
3. [bracket] / <bracket> → False (키워드 미매칭 시만 도달)
4. len > 80 → False
5. () 포함 → False
6. 다음 줄이 bullet(•) → True
7. 줄이 : 로 끝남 → True
8. 2 ≤ len ≤ 15 + 다음 줄 len ≥ 20 + 한글/영문 시작 → True
```

⚠️ **Rule 6 (bullet follows rule)**: 한국어 소제목(19자 이하)이 bullet 앞에 있으면 헤더로 판정됨
→ `detect_semantic_zone`에서 키워드 미매칭 시 "others" zone으로 빠질 수 있음

## Section dataclass

```python
@dataclass
class Section:
    header: str | None      # 섹션 제목
    lines: list[str]        # 본문 라인
    lists: list[list[str]]  # bullet 그룹 목록
    semantic_zone: str      # 기본 "others"
```

## normalize_header (section_builder)

```python
def normalize_header(header):
    return header.replace(" ", "")   # 공백 제거만, 대괄호 유지
```

e.g. `[Key Responsibilities]` → `[KeyResponsibilities]`
→ `detect_semantic_zone("[KeyResponsibilities]")` → "responsibilities" zone ✓
