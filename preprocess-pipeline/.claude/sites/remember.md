# 리멤버(Remember) 사이트 특성

## URL 패턴
- `career.remember.co.kr/job/XXXXX`
- React SPA → Playwright 필요

## 플랫폼 고유 섹션 레이블
```
주요업무 / 자격 요건 / 우대사항 / 채용절차 / 기타안내
```
이 레이블들은 Remember가 자동으로 붙이는 구조 레이블.
회사마다 내부에 자체 서브헤더(`[Key Responsibilities]` 등)를 추가하기도 함.

## 알려진 문제점 및 픽스

### 문제 1: best-candidate 알고리즘 실패
- 각 섹션(주요업무/자격요건/우대사항)이 별개의 sibling div에 렌더링됨
- 점수 기반 best-candidate이 하나만 선택 → 나머지 섹션 통째 누락
- **픽스**: `remember.co.kr` 감지 시 full-body extraction 사용
- **위치**: `url/parser.py` `_should_use_full_body_extraction()`

### 문제 2: [대괄호] 서브헤더 + 짧은 키워드 오탐
- `[Mission of the Role]` → `"role"`(4자) 부분일치 → 가짜 섹션 생성
- 결과: `주요업무` 섹션이 0줄이 되어 Rule 2 merge에 의존
- **픽스**: `bracket phrase + len(kw) < 6 + 부분일치 → skip`
- **위치**: `url/section_extractor.py`, `structural_preprocess/header_detector.py`

### 문제 3: `기타안내` 미인식
- Remember 고유 레이블이 `header_keywords.yml`에 없었음
- 결과: `기타안내` 이후 내용이 `채용절차` 섹션에 편입됨
- **픽스**: `기타안내`, `기타 안내` 추가
- **위치**: `common/section/header_keywords.yml`

## STRADVISION JD 구조 (Remember 대표 예시)

```
[About STRADVISION]       → __intro__ (키워드 미매칭)
  company content...
[Our Technology]          → __intro__ (키워드 미매칭)
  tech content...

주요업무                  → header (키워드 매칭)
[Mission of the Role]     → [픽스 후] NOT header → 주요업무 본문
  mission content...
[Key Responsibilities]    → header ("responsibilities" 16자)
  • bullet 1
  • bullet 2

자격 요건                 → header
[Basic Qualifications]    → header ("qualifications" 14자)
  • bullet 1

우대사항                  → header
[Preferred Qualifications]→ header ("preferred qualifications" 22자)
  • bullet 1

채용절차                  → header
  ▶️ 서류 > 면접 > ...

기타안내                  → [픽스 후] header (키워드 추가됨)
  안내 텍스트...
```

## 픽스 후 예상 canonical_map

```python
{
  "responsibilities": [mission content + key responsibilities bullets],
  "requirements":     [basic qualifications bullets],
  "preferred":        [preferred qualifications bullets],   ← 이전엔 empty
  "process":          [채용 절차 내용],
}
```

## 미해결 이슈

- 탭 기반 UI Remember 페이지: Playwright가 탭 클릭을 하지 않아 일부 섹션 미로드 가능
- 로그인 필요 JD: 인증 없이 접근 시 truncated 내용만 수집될 수 있음
