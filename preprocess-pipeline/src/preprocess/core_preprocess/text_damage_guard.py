import re

# Meaningful chars: English, number, Korean syllables.
_MEANING_CHAR_RE = re.compile(r"[A-Za-z0-9가-힣]")
# Special chars (exclude whitespace and meaningful chars).
_SPECIAL_CHAR_RE = re.compile(r"[^A-Za-z0-9가-힣\s]")


def _is_obviously_broken(line: str) -> bool:
    """
    Decide whether a line is clearly damaged/noisy.
    Keep this conservative to avoid dropping valid JD lines.
    """
    stripped = line.strip()
    if not stripped:
        return False

    # Very short non-meaningful line.
    if len(stripped) <= 3 and not _MEANING_CHAR_RE.search(stripped):
        return True

    total_len = len(stripped)
    meaning_count = len(_MEANING_CHAR_RE.findall(stripped))

    # Too few meaningful chars in longer lines.
    if total_len >= 5:
        ratio = meaning_count / total_len
        if ratio < 0.3:
            return True

    # Too many special chars.
    special_count = len(_SPECIAL_CHAR_RE.findall(stripped))
    if total_len >= 5 and (special_count / total_len) > 0.6:
        return True

    return False


def guard_text_damage(lines: list[str]) -> list[str]:
    """Filter out only clearly broken lines."""
    guarded: list[str] = []
    for line in lines:
        if _is_obviously_broken(line):
            continue
        guarded.append(line)
    return guarded

