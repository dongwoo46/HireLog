
def filter_low_quality_lines(
        lines: list[dict],
        min_confidence: int = 45,
        max_garbage_ratio: float = 0.6,
) -> tuple[list[dict], list[dict]]:
    """
    OCR 결과에 대한 라인 단위 품질 게이트.

    이 함수의 역할:
    - OCR 결과 중 명백히 신뢰하기 어려운 라인을 초기 단계에서 제거한다.
    - 제거된 라인은 이후 품질 기준 조정과 분석을 위해 별도로 보관한다.

    설계 원칙:
    - 이 단계에서는 텍스트를 교정하지 않는다.
    - JD 도메인 의미 판단을 하지 않는다.
    - 'OCR 실패 가능성이 높은 라인'만 보수적으로 차단한다.

    입력:
    - lines: normalize_lines 이후의 line 단위 OCR 결과

    출력:
    - passed_lines: 품질 기준을 통과한 라인 리스트
    - dropped_lines: 품질 기준에 의해 제거된 라인 리스트 (사유 포함)
    """

    passed_lines = []
    dropped_lines = []

    for line in lines:
        text = line.get("text", "").strip()

        # 빈 텍스트 라인은 OCR 실패로 간주하고 제거한다.
        if not text:
            dropped_lines.append({
                "reason": "empty_text",
                "line": line
            })
            continue

        tokens = text.split()

        # 토큰이 없는 라인은 의미 없는 결과로 간주한다.
        if not tokens:
            dropped_lines.append({
                "reason": "no_tokens",
                "line": line
            })
            continue

        # PaddleOCR 기준: line-level confidence 사용
        confidence = line.get("confidence_avg", 100)

        # OCR 엔진 자체가 신뢰하지 않는 라인은 초기에 차단한다.
        if confidence < min_confidence:
            dropped_lines.append({
                "reason": "low_confidence",
                "confidence": confidence,
                "line": line
            })
            continue

        # 라인 내 garbage 토큰 비율 계산
        garbage_count = sum(
            1 for token in tokens
            if _looks_garbage_token(token)
        )
        garbage_ratio = garbage_count / len(tokens)

        # 의미 있는 텍스트보다 잡음이 많은 라인은 제거한다.
        if garbage_ratio > max_garbage_ratio:
            dropped_lines.append({
                "reason": "high_garbage_ratio",
                "garbage_ratio": garbage_ratio,
                "line": line
            })
            continue

        # 모든 품질 기준을 통과한 라인
        passed_lines.append(line)

    return passed_lines, dropped_lines

def _looks_garbage_token(token: str) -> bool:
    """
    OCR 결과에서 'garbage 토큰'으로 판단할지 여부를 결정한다.

    이 함수의 역할:
    - OCR 오인식으로 생성된 의미 없는 토큰을 탐지한다.
    - JD에 등장할 수 있는 기술 키워드(k8s, gRPC, Node.js 등)는
      최대한 보존하는 것을 목표로 한다.

    판단 원칙:
    - 이 단계는 매우 보수적으로 동작해야 한다.
    - false positive(정상 토큰 제거)를 최소화한다.

    반환:
    - True  : garbage 토큰으로 간주
    - False : 의미 있는 토큰으로 간주
    """

    length = len(token)

    # 한 글자 토큰은 대부분 의미 없는 OCR 잡음이다.
    # (C, R 등의 예외는 상위 단계에서 보호하는 것을 전제로 한다)
    if length <= 1:
        return True

    # 알파벳/숫자 비율 계산
    # 기술 키워드는 보통 alphanumeric 비율이 높다.
    alnum_count = sum(c.isalnum() for c in token)
    alnum_ratio = alnum_count / length

    if alnum_ratio < 0.4:
        return True

    # 특수문자가 과반수 이상인 경우는
    # 깨진 OCR 결과일 가능성이 높다.
    special_count = sum(not c.isalnum() for c in token)
    if special_count > length * 0.5:
        return True

    return False
