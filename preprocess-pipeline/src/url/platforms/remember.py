# src/url/platforms/remember.py

"""
리멤버(Remember) 플랫폼 전처리

특성:
- 탭 네비게이션으로 헤더가 연속으로 먼저 등장 (주요업무/자격요건/우대사항/채용절차/기타안내)
- 이후 본문에서 같은 헤더가 다시 등장
- 탭 nav 제거 후 본문 헤더 보존 필요 → 중복 허용 + 탭 nav 제거

전략:
- header keyword 중복 허용 (dedup 제외)
- 연속 5개 이상 + 전부 header keyword → 탭 네비게이션 판단 → 전부 제거
- 혼합 버퍼(header + 비header)는 header keyword 라인만 보존
"""

from typing import List, Set

from common.section.loader import load_header_keywords


# 리멤버 플랫폼 전용 UI 노이즈 패턴
_REMEMBER_UI_NOISE_PATTERNS = [
    r"^이 포지션에 합격해 입사하시면",
    r"^합격 보상금",
    r"^먼저 입사한 실무자에게",
    r"^이 공고와 비슷한 공고",
    r"^리멤버에서 수집한 기업 정보",
    r"^정보 수정이 필요할 경우",
    r"^로그인하고 현직자에게",
    r"^사용자가 커넥트에 입력한",
]


def remove_menu_fragments(lines: List[str]) -> List[str]:
    """
    메뉴 잔해 패턴 제거 (리멤버 전략)

    연속 5개 이상 짧은 라인이 전부 header keyword → 탭 네비게이션으로 판단 → 전부 제거.
    혼합 버퍼는 header keyword 라인만 보존.
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
                if all(_is_header_keyword(l, header_keywords) for l in buffer):
                    # 탭 네비게이션 → 전부 제거
                    pass
                else:
                    for buf_line in buffer:
                        if _is_header_keyword(buf_line, header_keywords):
                            result.append(buf_line)
            else:
                result.extend(buffer)
            buffer = []
            result.append(line)

    if buffer:
        if len(buffer) >= 5:
            if all(_is_header_keyword(l, header_keywords) for l in buffer):
                pass
            else:
                for buf_line in buffer:
                    if _is_header_keyword(buf_line, header_keywords):
                        result.append(buf_line)
        else:
            result.extend(buffer)

    return result


def allow_header_keyword_dedup() -> bool:
    """리멤버는 탭 nav + 본문 헤더 중복 등장 → dedup 제외"""
    return True


def get_ui_noise_patterns() -> List[str]:
    return _REMEMBER_UI_NOISE_PATTERNS


def _is_header_keyword(line: str, header_keywords: Set[str]) -> bool:
    normalized = line.strip().lower().replace(" ", "")
    if not normalized:
        return False
    for kw in header_keywords:
        kw_normalized = kw.lower().replace(" ", "")
        if kw_normalized == normalized or kw_normalized in normalized:
            return True
    return False
