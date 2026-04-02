import re
from typing import Dict, List, Optional

from url.section_extractor import extract_url_sections

INTRO_KEY = "__intro__"

# Section labels are intentionally Korean-friendly for Saramin posts.
SECTION_KEYWORDS: Dict[str, List[str]] = {
    "주요업무": ["주요업무", "담당업무", "업무", "직무", "responsibilities", "role"],
    "자격요건": ["자격요건", "지원자격", "필수", "requirements", "qualifications", "required"],
    "우대사항": ["우대사항", "우대", "preferred", "nice to have", "plus"],
    "근무조건": ["근무조건", "고용형태", "근무시간", "근무지", "연봉", "급여", "employment", "location"],
    "복리후생": ["복리후생", "혜택", "복지", "benefits", "perks"],
    "전형절차": ["전형절차", "채용절차", "프로세스", "process"],
    "지원방법": ["지원방법", "접수방법", "제출서류", "접수기간", "apply"],
    "기술스택": ["기술스택", "스택", "tool", "tools", "stack"],
}

_SARAMIN_UI_NOISE_PATTERNS = [
    r"^TOP$",
    r"^이전공고\s*다음공고$",
    r"사람인\s*인공지능\s*기술",
    r"채용정보제공\s*서비스",
]

_HEADER_PREFIX_RE = re.compile(r"^[\s\-\*\u2022\u25cf\u25a0\u25b6\u25b7\u2605\u2713\u2714\U0001F4CC\U0001F4CB\U0001F3E0\U0001F6CE\u3010\u3011\[\]\(\)]+")
_MULTISPACE_RE = re.compile(r"\s+")


def remove_menu_fragments(lines: List[str]) -> List[str]:
    return [line for line in lines if line and line.strip()]


def allow_header_keyword_dedup() -> bool:
    return True


def get_ui_noise_patterns() -> List[str]:
    return _SARAMIN_UI_NOISE_PATTERNS


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

        # 1) heading style: "## 주요업무"
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

        # 2) plain heading style: "📋 주요업무", "- 자격요건", "[우대사항]"
        plain_matched = _match_section_by_text(text)
        if plain_matched and _looks_like_heading_line(text):
            current_section = plain_matched
            first_section_seen = True
            sections.setdefault(current_section, [])
            continue

        # 3) labeled row style: "자격요건: 경력 2년 이상"
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
            if _looks_like_company_name(text):
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
    t = _MULTISPACE_RE.sub(" ", t).strip().lower()
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
    t = (text or "").strip()
    if not t:
        return False
    # Heading lines are typically short labels.
    return len(t) <= 30


def _looks_like_company_name(text: str) -> bool:
    t = (text or "").strip()
    if len(t) > 40:
        return False
    return (
        "(주)" in t
        or "주식회사" in t
        or t.endswith("회사")
        or t.endswith("스타랩스")
        or t.endswith("아이타이쿤")
    )


def _is_fragment(text: str) -> bool:
    t = text.strip()
    if len(t) <= 2:
        return True
    if len(t) <= 12 and " " in t and all(part.isalpha() for part in t.split()):
        return True
    return False
