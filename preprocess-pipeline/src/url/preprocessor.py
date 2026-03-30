# src/url/preprocessor.py

"""
URL 전용 전처리기

역할:
- HTML 파싱 후 추출된 텍스트에서 노이즈 제거
- 웹 페이지 특유의 UI 요소, 버튼, 네비게이션 텍스트 제거
- platform별 전처리 전략 분기
"""

import re
from typing import List, Set

from common.section.loader import load_header_keywords
from domain.job_platform import JobPlatform


# ============================================================
# UI 노이즈 패턴 (공통)
# ============================================================

UI_NOISE_EXACT = {
    # 한국어
    "닫기", "열기", "펼치기", "접기",
    "공유", "공유하기", "저장", "저장하기",
    "지원하기", "바로지원", "간편지원", "즉시지원",
    "스크랩", "스크랩하기", "찜하기", "찜",
    "목록", "목록으로", "뒤로", "뒤로가기",
    "로그인", "회원가입", "마이페이지",
    "검색", "검색하기", "초기화", "필터",
    "더보기", "더 보기", "자세히", "자세히보기",
    "이전", "다음", "처음", "끝",
    "확인", "취소", "닫기",
    "복사", "링크복사", "URL복사",
    "신고", "신고하기", "문의", "문의하기",
    "좋아요", "추천", "조회수", "조회",

    # 영어
    "Close", "Open", "Expand", "Collapse",
    "Share", "Save", "Apply", "Apply Now",
    "Bookmark", "Like", "Follow",
    "Back", "Next", "Previous", "First", "Last",
    "Login", "Sign In", "Sign Up", "Register",
    "Search", "Filter", "Reset", "Clear",
    "More", "View More", "Read More", "See More",
    "OK", "Cancel", "Confirm",
    "Copy", "Copy Link", "Report",
}

UI_NOISE_PATTERNS_COMMON = [
    r"^\d+일\s*전$",
    r"^\d+시간\s*전$",
    r"^\d+분\s*전$",
    r"^조회\s*\d+",
    r"^D-\d+$",
    r"^마감\s*D-\d+",
    r"^\d+명\s*지원",
    r"^지원자\s*\d+명",
    r"^평점\s*[\d.]+",
    r"^★+",
    r"^⭐+",
    r"^©",
    r"^Copyright",
    r"^All rights reserved",
]

META_NOISE_PATTERNS = [
    r"^등록일\s*:?\s*\d{4}",
    r"^수정일\s*:?\s*\d{4}",
    r"^Posted\s*:?\s*\d",
    r"^Updated\s*:?\s*\d",
    r"^마감일\s*:?\s*\d{4}",
]


# ============================================================
# 플랫폼 모듈 로더
# ============================================================

def get_platform_module(platform: JobPlatform):
    if platform == JobPlatform.WANTED:
        from url.platforms import wanted
        return wanted
    elif platform == JobPlatform.REMEMBER:
        from url.platforms import remember
        return remember
    else:
        from url.platforms import generic
        return generic


# ============================================================
# 전처리 진입점
# ============================================================

def preprocess_lines_by_platform(lines: List[str], platform: JobPlatform = JobPlatform.OTHER) -> List[str]:
    """
    TEXT/OCR 파이프라인용 platform 전용 필터

    URL 전용 노이즈(HTML UI 요소 등)는 제거하지 않고
    platform 모듈의 noise pattern + menu fragment 제거만 적용
    """
    if not lines:
        return lines

    platform_mod = get_platform_module(platform)
    platform_noise_patterns = platform_mod.get_ui_noise_patterns()

    filtered = [
        line for line in lines
        if not (platform_noise_patterns and _matches_patterns(line, platform_noise_patterns))
    ]

    return platform_mod.remove_menu_fragments(filtered)


def preprocess_url_text(text: str, platform: JobPlatform = JobPlatform.OTHER) -> List[str]:
    """
    URL에서 파싱한 텍스트를 전처리하여 클린한 라인 리스트 반환

    Args:
        text: HTML 파싱 후 추출된 raw 텍스트
        platform: 채용 플랫폼 (전처리 전략 분기용)

    Returns:
        전처리된 라인 리스트
    """
    if not text:
        return []

    platform_mod = get_platform_module(platform)
    platform_noise_patterns = platform_mod.get_ui_noise_patterns()
    allow_header_dedup = platform_mod.allow_header_keyword_dedup()

    lines = text.split("\n")
    cleaned_lines = []
    seen_lines: Set[str] = set()
    header_keywords = load_header_keywords()

    for line in lines:
        # 1. 기본 정리
        line = _normalize_whitespace(line)
        if not line:
            continue

        # 2. UI 노이즈 제거 (완전 일치)
        if line in UI_NOISE_EXACT:
            continue
        if line.lower() in {n.lower() for n in UI_NOISE_EXACT}:
            continue

        # 3. 공통 패턴 기반 노이즈 제거
        if _matches_patterns(line, UI_NOISE_PATTERNS_COMMON + META_NOISE_PATTERNS):
            continue

        # 4. 플랫폼 전용 노이즈 제거
        if platform_noise_patterns and _matches_patterns(line, platform_noise_patterns):
            continue

        # 5. 너무 짧은 라인 제거
        if _is_too_short(line):
            continue

        # 6. 중복 라인 제거
        line_key = line.lower().replace(" ", "")
        if line_key in seen_lines:
            # 플랫폼이 header dedup 허용이고 header keyword인 경우 통과
            if not (allow_header_dedup and _is_header_keyword(line, header_keywords)):
                continue
        else:
            seen_lines.add(line_key)

        # 7. 특수문자 정리
        line = _clean_special_chars(line)
        if not line:
            continue

        cleaned_lines.append(line)

    # 8. 플랫폼별 메뉴 잔해 제거
    cleaned_lines = platform_mod.remove_menu_fragments(cleaned_lines)

    return cleaned_lines


# ============================================================
# 공통 유틸
# ============================================================

def _normalize_whitespace(line: str) -> str:
    line = re.sub(r"[\t\r]+", " ", line)
    line = re.sub(r" {2,}", " ", line)
    return line.strip()


def _matches_patterns(line: str, patterns: List[str]) -> bool:
    for pattern in patterns:
        if re.match(pattern, line, re.IGNORECASE):
            return True
    return False


def _is_too_short(line: str) -> bool:
    if len(line) <= 2:
        if any(c.isdigit() for c in line):
            return False
        return True
    if len(line) <= 3:
        if line.isalpha():
            return False
    return False


def _clean_special_chars(line: str) -> str:
    line = re.sub(r"[·•\-]{3,}", "•", line)
    line = re.sub(r"[=]{3,}", "", line)
    line = re.sub(r"[-]{3,}", "", line)
    return line.strip()


def _is_header_keyword(line: str, header_keywords: Set[str]) -> bool:
    normalized = line.strip().lower().replace(" ", "")
    if not normalized:
        return False
    for kw in header_keywords:
        kw_normalized = kw.lower().replace(" ", "")
        if kw_normalized == normalized or kw_normalized in normalized:
            return True
    return False
