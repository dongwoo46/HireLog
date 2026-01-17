
def _is_noise_line(
        line: str,
        patterns: dict[str, set[str]],
) -> bool:
    """
    JD와 무관한 명백한 UI / 시스템 노이즈 라인 판정

    원칙:
    - 라인 전체가 UI일 때만 제거
    - substring 제거 금지
    - 의미 추론 금지
    """

    text = line.strip()
    if not text:
        return False

    lowered = text.lower()

    # 1️⃣ exact
    if lowered in patterns.get("exact", set()):
        return True

    # 2️⃣ navigation 단독
    if lowered in patterns.get("navigation", set()):
        return True

    # 3️⃣ prefix (짧은 footer만)
    for prefix in patterns.get("prefix", set()):
        if lowered.startswith(prefix):
            if len(lowered) <= len(prefix) + 30:
                return True

    # 4️⃣ suffix (짧은 footer만)
    for suffix in patterns.get("suffix", set()):
        if lowered.endswith(suffix):
            if len(lowered) <= len(suffix) + 30:
                return True

    return False


def remove_ui_noise(text: str, patterns: dict) -> str:
    lines = text.split("\n")
    kept = [
        line
        for line in lines
        if not _is_noise_line(line, patterns)
    ]
    return "\n".join(kept)
