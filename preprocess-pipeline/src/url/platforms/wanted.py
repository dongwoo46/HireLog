# src/url/platforms/wanted.py

"""
원티드(Wanted) 플랫폼 전처리

특성:
- 섹션 헤더(주요업무/자격요건/우대사항/혜택 및 복지/채용 전형)가
  본문 내에서 독립 라인으로 등장
- 탭 네비게이션 없이 섹션 헤더가 한 번만 등장
- 헤더를 연속 그룹으로 제거하면 섹션 분리 불가 → 보존 필수

전략:
- 연속 5개 이상 짧은 라인 → header keyword 포함 라인만 보존 (generic과 동일)
- header keyword 중복 허용 안 함 (원티드는 중복 등장 없음)
"""

from typing import List, Set

from common.section.loader import load_header_keywords


def remove_menu_fragments(lines: List[str]) -> List[str]:
    """
    메뉴 잔해 패턴 제거 (원티드 전략)

    header keyword가 연속으로 등장해도 실제 섹션 헤더이므로 보존.
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
    """원티드는 헤더 중복이 없으므로 dedup 적용"""
    return False


def get_ui_noise_patterns() -> List[str]:
    """원티드 전용 UI 노이즈 패턴"""
    return [
        r"^채용중인 포지션",
        r"^지금 채용중",
        r"^연봉상위",
        r"^팔로우하고 채용알림",
        r"^기업 팔로우하면",
        r"^관심 기업으로 등록",
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
