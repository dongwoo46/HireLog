from typing import List, Set

from common.section.loader import load_header_keywords


def remove_menu_fragments(lines: List[str]) -> List[str]:
    """
    Remove short navigation/menu fragments for Wanted pages.

    Keep actual header keyword lines even when many short lines are clustered.
    """
    if len(lines) < 5:
        return lines

    header_keywords = load_header_keywords()
    result: list[str] = []
    buffer: list[str] = []

    for line in lines:
        if len(line) <= 10:
            buffer.append(line)
            continue

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
    """Wanted listings usually do not need duplicate header allowance."""
    return False


def extract_sections(lines: List[str]) -> dict:
    """Use shared URL section extractor for Wanted."""
    from url.section_extractor import extract_url_sections

    return extract_url_sections(lines)


def get_ui_noise_patterns() -> List[str]:
    """Wanted-specific UI/footer noise patterns."""
    return [
        r"^채용중인 회사",
        r"^지금 채용중",
        r"^연봉순위",
        r"^팔로우하고 채용알림",
        r"^기업 팔로우하면",
        r"^관심 기업으로 등록",
        r"^본 채용정보는 원티드랩의 동의없이",
        r"^본 채용 정보는",
        r".*무단전재.*재배포.*재가공.*",
        r".*구직활동 이외의 용도로 사용할 수 없습니다.*",
        r".*원티드랩의 저작자산이자 영업자산.*",
        r".*원티드랩의 동의 없이.*크롤링할 수 없으며.*",
        r".*원티드랩은.*어떠한 보장도 하지 않으며.*",
        r"^<저작권자 \(주\)원티드랩.*>$",
        r"^/OpenStreetMap$",
        r"^지도 데이터$",
        r"^포지션에 맞는 이력서로 다듬어 드려요$",
    ]


def _is_header_keyword(line: str, header_keywords: Set[str]) -> bool:
    normalized = line.strip().lower().replace(" ", "")
    if not normalized:
        return False

    for kw in header_keywords:
        kw_normalized = kw.lower().replace(" ", "")
        if kw_normalized == normalized or kw_normalized in normalized:
            return True

    return False
