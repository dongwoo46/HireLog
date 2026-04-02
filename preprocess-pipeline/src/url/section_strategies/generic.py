# src/url/platforms/generic.py

"""
Generic(기본) 플랫폼 전처리

원티드/리멤버 등 전용 로직이 없는 플랫폼에 적용.
연속 5개 이상 짧은 라인 중 header keyword 포함 라인만 보존.
"""

from typing import List, Set

from common.section.loader import load_header_keywords


def remove_menu_fragments(lines: List[str]) -> List[str]:
    """
    메뉴 잔해 패턴 제거 (기본 전략)

    연속된 짧은 라인(5개 이상) 중 header keyword 포함 라인만 보존.
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
            if len(buffer) >= 5:
                for buf_line in buffer:
                    if _is_header_keyword(buf_line, header_keywords):
                        result.append(buf_line)
            else:
                result.extend(buffer)
            buffer = []
            result.append(line)

    if buffer:
        if len(buffer) >= 5:
            for buf_line in buffer:
                if _is_header_keyword(buf_line, header_keywords):
                    result.append(buf_line)
        else:
            result.extend(buffer)

    return result


def allow_header_keyword_dedup() -> bool:
    """header keyword 중복 허용 여부 (기본: 허용 안 함)"""
    return False


def get_ui_noise_patterns() -> List[str]:
    """플랫폼 전용 UI 노이즈 패턴 (기본: 없음)"""
    return []


def extract_sections(lines: List[str]) -> dict:
    """섹션 분리 (기본 전략)"""
    from url.section_extractor import extract_url_sections
    return extract_url_sections(lines)


def _is_header_keyword(line: str, header_keywords: Set[str]) -> bool:
    normalized = line.strip().lower().replace(" ", "")
    if not normalized:
        return False
    for kw in header_keywords:
        kw_normalized = kw.lower().replace(" ", "")
        if kw_normalized == normalized or kw_normalized in normalized:
            return True
    return False
