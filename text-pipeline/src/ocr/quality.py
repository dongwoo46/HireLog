from typing import List, Dict


def filter_low_quality_lines(
        lines: List[Dict],
        min_confidence: int = 45,
        max_garbage_ratio: float = 0.6,
) -> List[Dict]:
    """
    OCR 라인 단위 품질 게이트

    목적:
    - 신뢰할 수 없는 라인을 초기 단계에서 차단
    - 교정 대상에서 제외 (제거 or 격리)

    정책:
    - 교정 ❌
    - 의미 판단 ❌
    """

    result = []

    for line in lines:
        text = line.get("text", "")
        confidence = line.get("confidence", 100)

        # 0️⃣ 빈 라인 제거
        if not text.strip():
            continue

        tokens = text.split()
        if not tokens:
            continue

        # 1️⃣ confidence 기준
        if confidence < min_confidence:
            continue

        # 2️⃣ garbage 토큰 비율
        garbage_count = sum(
            1 for token in tokens
            if _looks_garbage_token(token)
        )

        garbage_ratio = garbage_count / len(tokens)

        if garbage_ratio > max_garbage_ratio:
            continue

        # 통과
        result.append(line)

    return result



def _looks_garbage_token(token: str) -> bool:
    """
    라인 품질 판별용 garbage 토큰 판단

    매우 보수적:
    - 이 단계에서 실수하면 라인 전체가 날아감
    """

    # 너무 짧은 토큰
    if len(token) <= 1:
        return True

    # 알파벳 비율
    alpha_count = sum(c.isalpha() for c in token)
    alpha_ratio = alpha_count / len(token)

    if alpha_ratio < 0.4:
        return True

    # 특수문자 과다
    special_count = sum(not c.isalnum() for c in token)
    if special_count >= len(token) / 2:
        return True

    return False

