# URL 파이프라인

## 흐름

```
URL 입력
 └─ UrlFetcher.fetch()               → requests (정적)
     ├─ _needs_js_rendering() 판별
     └─ PlaywrightFetcher.fetch()     → JS 렌더링 필요 시 fallback
         └─ UrlParser.parse(html, url)
             └─ preprocess_url_text(body_text)
                 └─ extract_url_sections(cleaned_lines)
                     └─ validate_raw_sections()
                         └─ adapt_url_sections_to_sections()
                             └─ CanonicalSectionPipeline.process()
                                 ├─ apply_semantic_lite()
                                 ├─ filter_irrelevant_sections()
                                 └─ _build_canonical_map()   → canonical_map
```

## 주요 파일

| 파일 | 역할 |
|---|---|
| `url/fetcher.py` | requests 기반 정적 fetch |
| `url/playwright_fetcher.py` | JS 렌더링, 더보기 버튼 클릭 |
| `url/parser.py` | HTML → 텍스트, best-candidate 선정 |
| `url/preprocessor.py` | UI 노이즈 제거, 중복 제거 |
| `url/section_extractor.py` | 헤더 기반 섹션 분리 → `{header: [lines]}` |
| `preprocess/worker/pipeline/url_pipeline.py` | 위 단계 조합 |

## UrlParser 점수 기반 best-candidate

```python
score += len(text) * 0.1              # A. 텍스트 길이
score += len(long_paragraphs) * 100   # B. 50자 이상 문단 수 (영문 편향)
score += keyword_hits * 300           # C. JD 키워드 히트 (가장 강력)
# D. link_density > 0.3 → penalty
# E. short_line_density > 0.8 + keyword_hits < 2 → penalty × 0.5
```

**제거 태그**: `script, style, noscript, iframe, svg, path, header, footer, nav`

⚠️ `<header>` HTML 태그도 제거됨 → 일부 사이트에서 섹션 레이블 손실 가능

## full-body extraction 적용 도메인

```python
_FULL_BODY_DOMAINS = ["remember.co.kr", "career.remember.co.kr"]
```
best-candidate 대신 body 전체 텍스트 추출 → 섹션 분산 구조 대응

## Playwright 특성

- `domcontentloaded` → `networkidle` 대기 (max 10초)
- 페이지 스크롤 (lazy load 대응)
- "더보기" 버튼 자동 클릭 (`EXPAND_BUTTON_TEXTS`: 20여 개 패턴)
- ⚠️ **탭 네비게이션 클릭 미지원** → 탭형 JD 일부 섹션 누락 가능

## extract_url_sections 헤더 판별 (`_get_matched_header_keyword`)

1. `len > 50` → None
2. bullet(`•-·*▶▪○●`) 시작 → None
3. 숫자 시작 → None
4. `[...]` → 대괄호 제거 후 정규화
5. 키워드 매칭 (부분 일치)
   - **픽스**: `[bracket]` + `len(kw) < 6` + 부분일치 → skip (오탐 방지)
   - e.g. `[Mission of the Role]` + `"role"` → 제외
   - e.g. `[Key Responsibilities]` + `"responsibilities"` → 허용 (len=16)
6. `_looks_like_sentence()` 체크 (짧은 키워드)

## URL 전처리 (`preprocess_url_text`)

- `UI_NOISE_EXACT`: 완전 일치 제거 (닫기, 저장하기, 지원하기 등 40여 개)
- `UI_NOISE_PATTERNS`: 패턴 제거 (D-7, 3일 전, ★, © 등)
- 중복 라인 제거 (seen_lines set)
- `_remove_menu_fragments()`: 연속 짧은 라인(≤10자) 5개 이상 → 메뉴 잔해 제거, header keyword 포함 라인 보존
