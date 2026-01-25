import re

# OCR에서 자주 발생하는 제거 대상 문자 집합
OCR_NOISE_CHARS = r"[|=~^]"

def normalize_chars(text: str) -> str:
    """
    OCR 결과 문자열을 '문자 단위'로 정규화한다.

    책임:
    - OCR로 인해 분리된 한글 자간 병합
    - OCR 노이즈 특수문자 제거
    - 공백 형태 최소 정리

    주의:
    - 의미 단어 삭제 금지
    - 토큰 재구성 금지
    """

    # 1️⃣ OCR 노이즈 특수문자 제거
    text = re.sub(OCR_NOISE_CHARS, "", text)

    # 2️⃣ 한글-한글 사이에만 있는 과도한 공백 병합
    # (영문/숫자와 섞인 경우는 보존)
    text = re.sub(
        r"(?<=[가-힣])\s+(?=[가-힣])",
        "",
        text
    )

    # 3️⃣ 연속 공백 축소 (형태 정리 수준)
    text = re.sub(r"\s{2,}", " ", text)

    return text.strip()
