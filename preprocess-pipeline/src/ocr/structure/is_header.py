from common.section.loader import load_header_keywords

HEADER_MAX_LENGTH = 40
HEADER_MAX_TOKENS = 6


def is_ocr_header_line(line: dict) -> bool:
    """
    JD 헤더(header) 여부를 보수적으로 판정한다.

    판정 원칙:
    - 키워드 포함은 필수
    - 짧고 명확한 문구
    - 설명 문장 배제
    - OCR confidence 최소 기준
    - 시각적 신호(header_score / height)로 보강
    """

    text = line.get("text", "").strip()
    if not text:
        return False

    text_lower = text.lower()

    # 1️⃣ 너무 긴 문장은 header 아님
    if len(text) > HEADER_MAX_LENGTH:
        return False

    # 2️⃣ 토큰 수 제한 (헤더는 짧다)
    if line.get("token_count", 0) > HEADER_MAX_TOKENS:
        return False


    # 4️⃣ 문장형 배제
    if _looks_like_sentence(text_lower):
        return False

    # 5️⃣ 키워드 포함 여부 (필수 조건)
    header_keywords = load_header_keywords()
    if not any(kw in text_lower for kw in header_keywords):
        return False

    if text.startswith(("·", "-", "•", "*")):
        return False

    # 숫자 시작 라인 차단 (목차/순서 방어)
    if text[0].isdigit():
        return False

        # 6️⃣ 시각적 보강 조건
    if _visual_header_signal(line):
        return True

    # 키워드 + 기본 조건만 충족해도 header 인정
    return True

def _looks_like_sentence(text: str) -> bool:
    """
    설명 문장처럼 보이는 텍스트 제거
    """

    # 명확한 문장 종결
    if text.endswith(".") or text.endswith("다") or text.endswith("다."):
        return True

    # 설명형 조사/어미
    sentence_markers = [
        "합니다", "됩니다", "있습니다",
        "하는 ", "하며", "및 ",
        "으로 ", "에서 ", "하여 ",
    ]

    return any(marker in text for marker in sentence_markers)

def _visual_header_signal(line: dict) -> bool:
    """
    OCR 결과 기반 시각적 헤더 신호
    """

    # header_score는 최강 신호
    if line.get("header_score", 0) >= 4:
        return True

    # 상대적으로 큰 글자
    if line.get("height", 0) >= 45:
        return True

    # 영문 JD: 콜론 종료
    text = line.get("text", "")
    if text.endswith(":"):
        return True

    return False

def normalize_header_key(text: str) -> str:
    """
    OCR 헤더 텍스트를 '키로 쓰기 적합한 형태'로만 정규화한다.
    의미 변환은 하지 않는다.
    """

    return (
        text.strip()
        .lower()
        .replace(" ", "")
    )
