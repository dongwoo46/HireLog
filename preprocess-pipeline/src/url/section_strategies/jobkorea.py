import re
from typing import Dict, List, Optional

from url.section_extractor import extract_url_sections

INTRO_KEY = "__intro__"

SECTION_KEYWORDS: Dict[str, List[str]] = {
    "주요업무": ["주요업무", "담당업무", "업무내용", "직무", "responsibilities", "role"],
    "자격요건": ["자격요건", "지원자격", "필수", "requirements", "qualifications", "required"],
    "우대사항": ["우대사항", "우대", "preferred", "plus", "nice to have"],
    "근무조건": ["근무조건", "근무시간", "근무형태", "고용형태", "연봉", "급여", "employment"],
    "전형절차": ["전형절차", "채용절차", "process"],
    "지원방법": ["지원방법", "접수방법", "제출서류", "접수기간", "지원양식", "apply"],
}

_JOBKOREA_UI_NOISE_PATTERNS = [
    r"잡코리아.*무단전재",
    r"불법/허위/과장/오류 신고",
    r"추천.?공고",
    r"AI추천공고",
    r"이 기업의 취업 전략",
    r"인적성.?면접 후기",
    r"관련 태그",
]

_HEADER_PREFIX_RE = re.compile(r"^[\s\-\*\u2022\u25cf\u25a0\u25b6\u25b7\u2605\u2713\u2714\U0001F4CC\U0001F4CB\u3010\u3011\[\]\(\)]+")


def remove_menu_fragments(lines: List[str]) -> List[str]:
    return [line for line in lines if line and line.strip()]


def allow_header_keyword_dedup() -> bool:
    return True


def get_ui_noise_patterns() -> List[str]:
    return _JOBKOREA_UI_NOISE_PATTERNS


def extract_sections(lines: List[str]) -> Dict[str, List[str]]:
    if not lines:
        return {}

    sections: Dict[str, List[str]] = {INTRO_KEY: []}
    current_section = INTRO_KEY
    first_section_seen = False

    for raw in lines:
        text = (raw or "").strip()
        if not text:
            continue

        if text.startswith("## "):
            heading = text[3:].strip()
            matched = _match_section_by_text(heading)
            if matched:
                current_section = matched
                first_section_seen = True
                sections.setdefault(current_section, [])
            else:
                current_section = INTRO_KEY
            continue

        plain_matched = _match_section_by_text(text)
        if plain_matched and _looks_like_heading_line(text):
            current_section = plain_matched
            first_section_seen = True
            sections.setdefault(current_section, [])
            continue

        if ":" in text:
            label = text.split(":", 1)[0].strip()
            matched = _match_section_by_text(label)
            if matched:
                sections.setdefault(matched, [])
                sections[matched].append(text)
                current_section = matched
                first_section_seen = True
                continue

        if not first_section_seen:
            if _looks_like_intro(text):
                sections[INTRO_KEY].append(text)
            continue

        if _is_fragment(text):
            continue

        sections.setdefault(current_section, [])
        sections[current_section].append(text)

    if not sections[INTRO_KEY]:
        sections.pop(INTRO_KEY, None)

    mapped_content = sum(len(v) for k, v in sections.items() if k != INTRO_KEY)
    if mapped_content == 0:
        return extract_url_sections(lines)

    return sections


def _normalize_text(text: str) -> str:
    t = _HEADER_PREFIX_RE.sub("", text or "")
    t = re.sub(r"[【】\[\]\(\)]", " ", t)
    t = re.sub(r"\s+", " ", t).strip().lower()
    return t.replace(" ", "")


def _match_section_by_text(text: str) -> Optional[str]:
    norm = _normalize_text(text)
    if not norm:
        return None
    for section, keywords in SECTION_KEYWORDS.items():
        for kw in keywords:
            if _normalize_text(kw) in norm:
                return section
    return None


def _looks_like_heading_line(text: str) -> bool:
    return len((text or "").strip()) <= 40


def _looks_like_intro(text: str) -> bool:
    t = (text or "").strip()
    if len(t) > 80:
        return False
    intro_kw = ("채용", "모집", "본부", "포지션", "개발")
    return any(k in t for k in intro_kw)


def _is_fragment(text: str) -> bool:
    t = (text or "").strip()
    if len(t) <= 2:
        return True
    if len(t) <= 12 and " " in t and all(part.isalpha() for part in t.split()):
        return True
    return False

