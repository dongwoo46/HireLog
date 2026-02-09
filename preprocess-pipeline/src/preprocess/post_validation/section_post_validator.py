# src/preprocess/post_validation/section_post_validator.py

"""
섹션 구조 후보정 (Post-Validation)

3개 파이프라인(OCR, URL, TEXT) 공통으로 적용되는
header 판정 결과 보정 로직.

적용 규칙:
  Rule 1 (intro 흡수)
    - 문서 시작부의 키워드 미매칭 header → intro로 강제 전환
    - 회사명, 직무 타이틀 등이 visual signal만으로 header 판정된 케이스 보정

  Rule 2 (빈 header 병합)
    - content 0줄인 header → 다음 header를 content로 흡수
    - "채용전형"(0줄) → "[채용절차]"(0줄) → "서류전형-코딩테스트..."(N줄) 패턴 보정

  Rule 3 (푸터 노이즈 제거)
    - 마지막 섹션 끝부분의 짧은 플랫폼 태그/배지 라인 제거
    - "커피·스낵바", "AI 선도기업", "적극채용중" 등

적용 순서: Rule 1 → Rule 2 → Rule 3 (순서 의존)
"""

import logging
from collections import OrderedDict

from common.section.loader import load_header_keywords

logger = logging.getLogger(__name__)

INTRO_KEY = "__intro__"
FOOTER_MAX_LINE_LENGTH = 15
FOOTER_MIN_CONSECUTIVE = 2
KEYWORD_COVERAGE_THRESHOLD = 0.4


# ──────────────────────────────────────────────
# Public API
# ──────────────────────────────────────────────

def validate_raw_sections(
    raw_sections: dict[str, list[str]],
) -> dict[str, list[str]]:
    """
    dict[header_key, lines] 구조에 대한 후보정

    OCR, URL 파이프라인에서 사용한다.
    """
    result = raw_sections

    result = _absorb_non_keyword_intro(result)
    result = _merge_empty_headers(result)
    result = _strip_footer_noise(result)

    return result


def validate_section_objects(sections: list) -> list:
    """
    list[Section] 구조에 대한 후보정

    TEXT 파이프라인에서 사용한다.
    Section(header, lines, lists, semantic_zone) 객체 리스트를
    dict로 변환 → 보정 → Section으로 복원한다.
    """
    from preprocess.structural_preprocess.section_builder import Section

    # Section → dict 변환
    raw: dict[str, list[str]] = OrderedDict()
    for sec in sections:
        key = sec.header if sec.header else INTRO_KEY
        # lines + lists 내 항목을 합산
        all_lines = list(sec.lines)
        for lst in sec.lists:
            all_lines.extend(lst)
        raw[key] = all_lines

    # 후보정 실행
    validated = validate_raw_sections(raw)

    # dict → Section 복원
    result = []
    for key, lines in validated.items():
        if key == INTRO_KEY:
            result.append(Section(
                header=None,
                lines=lines,
                lists=[],
                semantic_zone="intro",
            ))
        else:
            result.append(Section(
                header=key,
                lines=lines,
                lists=[],
                semantic_zone="others",
            ))

    return result


# ──────────────────────────────────────────────
# Rule 1: 키워드 미매칭 초반 header → intro 흡수
# ──────────────────────────────────────────────

def _absorb_non_keyword_intro(
    sections: dict[str, list[str]],
) -> dict[str, list[str]]:
    entries = list(sections.items())
    if not entries:
        return sections

    intro_lines: list[str] = []
    first_keyword_idx = len(entries)

    for i, (key, lines) in enumerate(entries):
        if key == INTRO_KEY:
            intro_lines.extend(lines)
            continue

        if _matches_any_keyword(key):
            first_keyword_idx = i
            break

        # 키워드 미매칭 header → intro로 강제
        logger.debug(
            "[POST_VALIDATE] Rule1: header \"%s\" → intro 흡수", key,
        )
        intro_lines.append(key)
        intro_lines.extend(lines)
        first_keyword_idx = i + 1

    result: dict[str, list[str]] = OrderedDict()

    if intro_lines:
        result[INTRO_KEY] = intro_lines

    for key, lines in entries[first_keyword_idx:]:
        result[key] = lines

    return result


# ──────────────────────────────────────────────
# Rule 2: content 0줄 header → 다음 header를 content로 흡수
# ──────────────────────────────────────────────

def _merge_empty_headers(
    sections: dict[str, list[str]],
) -> dict[str, list[str]]:
    entries = list(sections.items())
    result: list[tuple[str, list[str]]] = []
    i = 0

    while i < len(entries):
        key, lines = entries[i]

        # intro이거나 content가 있으면 그대로 유지
        if key == INTRO_KEY or lines:
            result.append((key, lines))
            i += 1
            continue

        # content 0줄 header → 후속 header를 content로 흡수
        merged_lines: list[str] = []
        j = i + 1

        while j < len(entries):
            next_key, next_lines = entries[j]
            # 후속 header의 key를 content 라인으로 편입
            merged_lines.append(next_key)
            merged_lines.extend(next_lines)
            j += 1
            if next_lines:
                # content가 있는 header를 만나면 흡수 종료
                break

        logger.debug(
            "[POST_VALIDATE] Rule2: 빈 header \"%s\" ← %d개 후속 header 흡수",
            key, j - i - 1,
        )

        result.append((key, merged_lines))
        i = j

    return OrderedDict(result)


# ──────────────────────────────────────────────
# Rule 3: 마지막 섹션 푸터 노이즈 제거
# ──────────────────────────────────────────────

def _strip_footer_noise(
    sections: dict[str, list[str]],
) -> dict[str, list[str]]:
    entries = list(sections.items())
    if not entries:
        return sections

    last_key, last_lines = entries[-1]
    if not last_lines:
        return sections

    # 끝에서부터 연속된 짧은 라인 탐색
    cutoff = len(last_lines)
    for i in range(len(last_lines) - 1, -1, -1):
        line = last_lines[i].strip()
        if len(line) > FOOTER_MAX_LINE_LENGTH:
            break
        cutoff = i

    removed = len(last_lines) - cutoff
    if removed >= FOOTER_MIN_CONSECUTIVE:
        logger.debug(
            "[POST_VALIDATE] Rule3: 마지막 섹션 \"%s\" 푸터 %d줄 제거",
            last_key, removed,
        )
        entries[-1] = (last_key, last_lines[:cutoff])

    return OrderedDict(entries)


# ──────────────────────────────────────────────
# Internal Helpers
# ──────────────────────────────────────────────

def _normalize_for_match(text: str) -> str:
    """키워드 매칭용 정규화 (소문자, 공백/괄호 제거)"""
    s = text.strip().lower().replace(" ", "")
    for ch in "[]<>【】":
        s = s.replace(ch, "")
    return s


def _matches_any_keyword(header_key: str) -> bool:
    """
    header_key가 header_keywords 중 하나와 매칭되는지 판정

    단순 부분 매칭이 아니라, 키워드가 텍스트의 유의미한 비중을
    차지하는지 확인한다. (coverage >= 40%)

    예:
      "자격요건" vs keyword "자격요건" → 4/4 = 100% → True
      "커넥트웨이브·서울금천구·신입-경력0년" vs "경력" → 2/19 = 10% → False
    """
    keywords = load_header_keywords()
    normalized = _normalize_for_match(header_key)

    if not normalized:
        return False

    for kw in keywords:
        kw_norm = kw.replace(" ", "")
        if kw_norm in normalized or normalized in kw_norm:
            coverage = len(kw_norm) / len(normalized)
            if coverage >= KEYWORD_COVERAGE_THRESHOLD:
                return True

    return False
