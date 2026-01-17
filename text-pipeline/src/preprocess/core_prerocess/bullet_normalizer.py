import re
from typing import List


# 다양한 bullet / 번호 표현
_BULLET_PATTERN = re.compile(
    r"""
    ^(?P<indent>\s*)
    (
        [•·\-\–\—\*] |
        \d+[\.\)] |
        \(\d+\) |
        [①②③④⑤⑥⑦⑧⑨⑩] |
        [가나다라마바사아자차카타파하][\.\)]
    )
    \s+
    (?P<content>.+)
    """,
    re.VERBOSE,
)


STANDARD_BULLET = "• "


def normalize_bullets(lines: List[str]) -> List[str]:
    """
    Core Preprocessing - Bullet / List Normalization

    정책:
    - bullet prefix만 통일
    - 본문 텍스트 변경 금지
    - 들여쓰기 보존
    """

    normalized: List[str] = []

    for line in lines:
        match = _BULLET_PATTERN.match(line)
        if not match:
            normalized.append(line)
            continue

        indent = match.group("indent")
        content = match.group("content")

        normalized.append(f"{indent}{STANDARD_BULLET}{content}")

    return normalized
