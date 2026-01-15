def classify_confidence(confidence: float) -> str:
    """
    OCR 평균 confidence를 기준으로
    결과 상태를 분류한다.

    기준은 실험을 통해 조정 가능.

    GOOD  : 바로 사용 가능 (LLM, normalize 가능)
    RETRY : 재전처리 / 재시도 고려
    FAIL  : 결과 폐기 또는 사용자 재업로드 요청
    """

    if confidence >= 85:
        return "GOOD"

    if confidence >= 60:
        return "RETRY"

    return "FAIL"
