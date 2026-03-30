# 리멤버 채용공고 파싱 개선

**작업일**: 2026-03-27 (커밋 f57c785 ~ 63c9299)
**이슈**: remember.co.kr 채용공고에서 `preferred` 등 섹션 누락 → `isValidJd()` 차단

---

## 문제 원인 3가지

### 1. best-candidate 알고리즘 실패 (parser.py)
리멤버는 각 섹션(주요업무/자격요건/우대사항)을 sibling div로 렌더링.
점수 기반 best-candidate 선택 로직이 하나의 div만 선택 → 나머지 섹션 통째 누락.

**픽스**: `remember.co.kr` URL 감지 → full-body extraction으로 전환.

### 2. `[대괄호]` + 짧은 키워드 오탐 (section_extractor, header_detector)
`[Mission of the Role]` → "role"(4자) 부분일치 → 가짜 섹션 생성.
`주요업무`가 0줄 섹션이 되어 merge 필요.

**픽스**: bracket phrase + len < 6 + 부분일치 → skip.

### 3. `기타안내` 미인식 (header_keywords.yml)
리멤버 고유 섹션 레이블이 키워드 목록에 없어 섹션 미분리.

**픽스**: `기타안내`, `기타 안내` 추가.

---

## 수정 파일

| 파일 | 변경 |
|---|---|
| `url/parser.py` | `_FULL_BODY_DOMAINS`에 `remember.co.kr` 추가, full-body extraction 분기 |
| `preprocess/worker/pipeline/url_pipeline.py` | `parser.parse(html, url=input.url)` — url 전달 |
| `url/section_extractor.py` | bracket+짧은키워드 부분일치 오탐 방지 로직 |
| `structural_preprocess/header_detector.py` | 동일 픽스 (TEXT/OCR 경로) |
| `common/section/header_keywords.yml` | `기타안내`, `기타 안내` 추가 |

---

## 리멤버 HTML 구조 특성

- 섹션 헤더(주요업무/자격요건/우대사항/채용절차/기타안내)가 탭 nav에 연속으로 등장
- 이후 본문에서 같은 헤더가 다시 등장 (중복)
- Playwright `더보기` 버튼 자동 클릭으로 전체 내용 확장 필요

---

## 후속 작업 (2026-03-30)

리멤버 픽스로 추가된 `_remove_menu_fragments` 로직이 원티드를 깨뜨려
→ platform 기반 전처리 분리로 해결 (platform-based-preprocessing.md 참조)
