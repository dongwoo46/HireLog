# src/url/section_extractor.py

"""
URL 전용 섹션 분리기

역할:
- 전처리된 라인 리스트에서 header 기반으로 섹션 분리
- OCR의 extract_sections_by_header와 동일한 출력 형식
- URL 특성에 맞게 header 판정 로직 조정

출력 형식:
    {
        "주요업무": ["업무 내용 1", "업무 내용 2", ...],
        "자격요건": ["자격 1", "자격 2", ...],
        "우대사항": ["우대 1", "우대 2", ...],
        ...
    }
"""

from typing import Dict, List
from common.section.loader import load_header_keywords

INTRO_KEY = "__intro__"

# Header 판정 상수
HEADER_MAX_LENGTH = 50  # header는 보통 짧음 (여유 확보)
HEADER_MIN_LENGTH = 2


def extract_url_sections(lines: List[str]) -> Dict[str, List[str]]:
    """
    전처리된 URL 라인 목록에서 header 기반으로 섹션 분리

    Args:
        lines: 전처리된 라인 리스트

    Returns:
        {header_key: [line1, line2, ...]} 형태의 딕셔너리
    """
    sections: Dict[str, List[str]] = {
        INTRO_KEY: []
    }

    current_header: str | None = None
    header_keywords = load_header_keywords()

    for line in lines:
        if not line:
            continue

        # 1. Header 판정
        matched_keyword = _get_matched_header_keyword(line, header_keywords)
        if matched_keyword:
            header_key = _normalize_header_key(matched_keyword)
            current_header = header_key
            sections.setdefault(current_header, [])
            continue

        # 2. header 이전 → intro
        if current_header is None:
            sections[INTRO_KEY].append(line)
            continue

        # 3. body → 현재 header에 귀속
        sections[current_header].append(line)

    # intro가 비어 있으면 제거
    if not sections[INTRO_KEY]:
        sections.pop(INTRO_KEY)

    return sections


def _get_matched_header_keyword(line: str, header_keywords: set) -> str | None:
    """
    라인에서 매칭되는 header keyword를 찾아 반환

    Returns:
        매칭된 keyword 문자열 또는 None
    """
    stripped = line.strip()
    if not stripped:
        return None

    # 0. 길이 체크 - 너무 길면 본문
    if len(stripped) > HEADER_MAX_LENGTH:
        return None

    if len(stripped) < HEADER_MIN_LENGTH:
        return None

    # 1. bullet로 시작하면 제외
    if stripped.startswith(("•", "-", "·", "*", "▶", "▪", "○", "●")):
        return None

    # 2. 숫자로 시작 (목차/순서) → 제외
    if stripped[0].isdigit():
        return None

    # 3. 정규화 (공백 제거, 소문자)
    normalized = stripped.lower().replace(" ", "")

    # 4. 대괄호/꺾쇠 제거
    if normalized.startswith("[") and normalized.endswith("]"):
        normalized = normalized[1:-1]
    elif normalized.startswith("<") and normalized.endswith(">"):
        normalized = normalized[1:-1]

    # 5. 키워드 매칭 (공백 제거된 버전으로 비교)
    for kw in header_keywords:
        kw_normalized = kw.lower().replace(" ", "")
        # 완전 일치 또는 포함
        if kw_normalized == normalized or kw_normalized in normalized:
            # 키워드 길이가 충분히 길면 (토스 스타일) 문장형 검사 스킵
            # 짧은 키워드는 오탐 방지를 위해 문장형 검사 유지
            if len(kw_normalized) >= 6 or not _looks_like_sentence(stripped.lower()):
                return kw

    return None


def _looks_like_sentence(text: str) -> bool:
    """설명 문장처럼 보이는 텍스트 판정"""
    # 마침표로 끝남
    if text.endswith("."):
        return True

    # 너무 길면 문장
    if len(text) > 25:
        return True

    # 한국어 문장 종결
    if text.endswith(("다", "요", "음", "함")):
        if len(text) > 15:
            return True

    # 설명형 어미/조사
    sentence_markers = [
        "합니다", "됩니다", "있습니다", "입니다",
        "하는 ", "하며 ", "및 ",
        "으로 ", "에서 ", "하여 ",
        "것입니다", "바랍니다",
    ]

    return any(marker in text for marker in sentence_markers)


def _normalize_header_key(text: str) -> str:
    """
    Header 텍스트를 키로 정규화

    - 대괄호/꺾쇠 제거
    - 공백 제거
    - 소문자 변환
    """
    text = text.strip()

    # 대괄호/꺾쇠 제거
    if text.startswith("[") and text.endswith("]"):
        text = text[1:-1]
    elif text.startswith("<") and text.endswith(">"):
        text = text[1:-1]

    return text.strip().lower().replace(" ", "")
