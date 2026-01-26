# src/url/preprocessor.py

"""
URL 전용 전처리기

역할:
- HTML 파싱 후 추출된 텍스트에서 노이즈 제거
- 웹 페이지 특유의 UI 요소, 버튼, 네비게이션 텍스트 제거
- JD 본문에 집중할 수 있도록 클리닝

OCR/TEXT 전처리와 다른 점:
- 웹 UI 노이즈가 많음 (버튼, 링크, 메뉴 등)
- 중복 텍스트가 자주 발생 (헤더/푸터 반복)
- 짧은 단어 나열이 많음 (태그, 카테고리 등)
"""

import re
from typing import List, Set

from common.section.loader import load_header_keywords


# ============================================================
# UI 노이즈 패턴
# ============================================================

# 완전 일치로 제거할 UI 텍스트
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

# 부분 일치로 제거할 패턴 (시작/포함)
UI_NOISE_PATTERNS = [
    r"^\d+일\s*전$",           # "3일 전", "7일 전"
    r"^\d+시간\s*전$",         # "2시간 전"
    r"^\d+분\s*전$",           # "30분 전"
    r"^조회\s*\d+",            # "조회 123"
    r"^D-\d+$",                # "D-7", "D-30"
    r"^마감\s*D-\d+",          # "마감 D-7"
    r"^\d+명\s*지원",          # "123명 지원"
    r"^지원자\s*\d+명",        # "지원자 50명"
    r"^평점\s*[\d.]+",         # "평점 4.5"
    r"^★+",                    # 별점
    r"^⭐+",                   # 별점 이모지
    r"^©",                     # 저작권
    r"^Copyright",             # 저작권 영문
    r"^All rights reserved",   # 저작권
]

# 불필요한 메타 정보 패턴
META_NOISE_PATTERNS = [
    r"^등록일\s*:?\s*\d{4}",   # "등록일: 2024.01.01"
    r"^수정일\s*:?\s*\d{4}",   # "수정일: 2024.01.01"
    r"^Posted\s*:?\s*\d",      # "Posted: Jan 1"
    r"^Updated\s*:?\s*\d",     # "Updated: Jan 1"
    r"^마감일\s*:?\s*\d{4}",   # "마감일: 2024.12.31"
]


# ============================================================
# 전처리 함수
# ============================================================

def preprocess_url_text(text: str) -> List[str]:
    """
    URL에서 파싱한 텍스트를 전처리하여 클린한 라인 리스트 반환

    Args:
        text: HTML 파싱 후 추출된 raw 텍스트

    Returns:
        전처리된 라인 리스트
    """
    if not text:
        return []

    lines = text.split("\n")
    cleaned_lines = []
    seen_lines = set()  # 중복 제거용

    for line in lines:
        # 1. 기본 정리
        line = _normalize_whitespace(line)
        if not line:
            continue

        # 2. UI 노이즈 제거 (완전 일치)
        if line in UI_NOISE_EXACT:
            continue

        # 3. UI 노이즈 제거 (대소문자 무시)
        if line.lower() in {n.lower() for n in UI_NOISE_EXACT}:
            continue

        # 4. 패턴 기반 노이즈 제거
        if _matches_noise_pattern(line):
            continue

        # 5. 너무 짧은 라인 제거 (의미 없는 단어)
        if _is_too_short(line):
            continue

        # 6. 중복 라인 제거
        line_key = line.lower().replace(" ", "")
        if line_key in seen_lines:
            continue
        seen_lines.add(line_key)

        # 7. 특수문자 정리
        line = _clean_special_chars(line)
        if not line:
            continue

        cleaned_lines.append(line)

    # 8. 연속된 빈 줄/짧은 줄 패턴 제거 (메뉴 잔해)
    cleaned_lines = _remove_menu_fragments(cleaned_lines)

    return cleaned_lines


def _normalize_whitespace(line: str) -> str:
    """공백 정규화"""
    # 탭, 다중 공백을 단일 공백으로
    line = re.sub(r"[\t\r]+", " ", line)
    line = re.sub(r" {2,}", " ", line)
    return line.strip()


def _matches_noise_pattern(line: str) -> bool:
    """패턴 기반 노이즈 매칭"""
    for pattern in UI_NOISE_PATTERNS + META_NOISE_PATTERNS:
        if re.match(pattern, line, re.IGNORECASE):
            return True
    return False


def _is_too_short(line: str) -> bool:
    """
    의미 없이 짧은 라인 판정

    예외:
    - 숫자 포함 (연봉, 경력 등)
    - 특정 키워드 (header 가능성)
    """
    # 2글자 이하
    if len(line) <= 2:
        # 숫자 포함이면 보존 ("3년", "5급" 등)
        if any(c.isdigit() for c in line):
            return False
        return True

    # 3글자 이하 + 한글/영문만 있는 경우
    if len(line) <= 3:
        if line.isalpha():
            # header 키워드일 수 있으므로 보존
            # 이건 나중에 header 판정에서 처리
            return False

    return False


def _clean_special_chars(line: str) -> str:
    """특수문자 정리"""
    # 이모지 제거 (선택적)
    # line = re.sub(r'[\U00010000-\U0010ffff]', '', line)

    # 연속된 특수문자 정리
    line = re.sub(r"[·•\-]{3,}", "•", line)  # "•••" → "•"
    line = re.sub(r"[=]{3,}", "", line)       # "===" 제거
    line = re.sub(r"[-]{3,}", "", line)       # "---" 제거

    return line.strip()


def _remove_menu_fragments(lines: List[str]) -> List[str]:
    """
    메뉴 잔해 패턴 제거

    연속된 짧은 라인(5개 이상)이 모두 10자 이하면 메뉴 잔해로 간주
    단, header keyword가 포함된 라인은 보존
    """
    if len(lines) < 5:
        return lines

    header_keywords = load_header_keywords()
    result = []
    buffer = []

    for line in lines:
        if len(line) <= 10:
            buffer.append(line)
        else:
            # 버퍼에 쌓인 짧은 라인들 처리
            if len(buffer) >= 5:
                # 5개 이상 연속 짧은 라인 → 메뉴 잔해로 판단
                # 단, header keyword 포함 라인은 보존
                for buf_line in buffer:
                    if _is_header_keyword(buf_line, header_keywords):
                        result.append(buf_line)
            else:
                # 5개 미만이면 보존
                result.extend(buffer)

            buffer = []
            result.append(line)

    # 마지막 버퍼 처리
    if buffer:
        if len(buffer) >= 5:
            # header keyword 포함 라인만 보존
            for buf_line in buffer:
                if _is_header_keyword(buf_line, header_keywords):
                    result.append(buf_line)
        else:
            result.extend(buffer)

    return result


def _is_header_keyword(line: str, header_keywords: Set[str]) -> bool:
    """
    라인이 header keyword를 포함하는지 검사

    - 공백 제거, 소문자 변환 후 비교
    - 부분 일치 허용
    """
    normalized = line.strip().lower().replace(" ", "")
    if not normalized:
        return False

    for kw in header_keywords:
        kw_normalized = kw.lower().replace(" ", "")
        if kw_normalized == normalized or kw_normalized in normalized:
            return True

    return False
