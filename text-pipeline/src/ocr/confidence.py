from enum import Enum

# OCR confidence thresholds
GOOD_THRESHOLD = 85.0
RETRY_THRESHOLD = 60.0


class OcrConfidenceStatus(Enum):
    GOOD = "GOOD"
    RETRY = "RETRY"
    FAIL = "FAIL"


def classify_confidence(confidence: float) -> OcrConfidenceStatus:
    """
    OCR 평균 confidence를 기준으로 결과 상태를 분류한다.

    GOOD  : 바로 사용 가능 (LLM, normalize 가능)
    RETRY : 재전처리 / 재시도 고려
    FAIL  : 결과 폐기 또는 사용자 재업로드 요청
    """

    if confidence >= GOOD_THRESHOLD:
        return OcrConfidenceStatus.GOOD

    if confidence >= RETRY_THRESHOLD:
        return OcrConfidenceStatus.RETRY

    return OcrConfidenceStatus.FAIL
