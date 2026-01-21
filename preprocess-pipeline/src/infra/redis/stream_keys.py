# src/infra/redis/stream_keys.py

class JdStreamKeys:
    """
    Redis Stream Keys (JD Preprocess)

    설계 원칙:
    - 요청 / 결과 분리
    - TEXT / OCR 물리적 분리
    - Spring ↔ Python 공통 계약
    """

    # ==================================================
    # PREPROCESS REQUEST (Spring → Python)
    # ==================================================

    # TEXT 기반 JD 전처리 요청
    PREPROCESS_TEXT_REQUEST = "jd:preprocess:text:request:stream"

    # OCR 기반 JD 전처리 요청
    PREPROCESS_OCR_REQUEST = "jd:preprocess:ocr:request:stream"

    # ==================================================
    # PREPROCESS RESULT (Python → Spring)
    # ==================================================

    # TEXT 전처리 결과
    PREPROCESS_TEXT_RESPONSE = "jd:preprocess:text:response:stream"

    # OCR 전처리 결과
    PREPROCESS_OCR_RESPONSE = "jd:preprocess:ocr:response:stream"
