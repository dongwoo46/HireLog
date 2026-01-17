import re
from typing import List


_MEANING_CHAR_RE = re.compile(r"[A-Za-z0-9가-힣]")
_SPECIAL_CHAR_RE = re.compile(r"[^A-Za-z0-9가-힣\s]")


def _is_obviously_broken(line: str) -> bool:
    """
    형태 기준으로 '의미가 거의 없는' 라인인지 판별
    """

    stripped = line.strip()
    if not stripped:
        return False  # 빈 줄은 구조 신호 → 제거 금지

    # 1️⃣ 매우 짧은 라인
    if len(stripped) <= 3:
        # 의미 문자가 거의 없으면 제거 후보
        if not _MEANING_CHAR_RE.search(stripped):
            return True

    # 2️⃣ 의미 문자 비율
    total_len = len(stripped)
    meaning_count = len(_MEANING_CHAR_RE.findall(stripped))

    if total_len >= 5:
        ratio = meaning_count / total_len
        if ratio < 0.3:
            return True

    # 3️⃣ 특수문자 폭주
    special_count = len(_SPECIAL_CHAR_RE.findall(stripped))
    if total_len >= 5 and (special_count / total_len) > 0.6:
        return True

    return False


def guard_text_damage(lines: List[str]) -> List[str]:
    """
    Core Preprocessing - Text Damage Guard

    정책:
    - 명백히 무의미한 라인만 제거
    - 나머지는 전부 보존
    """

    guarded: List[str] = []

    for line in lines:
        if _is_obviously_broken(line):
            continue
        guarded.append(line)

    return guarded
